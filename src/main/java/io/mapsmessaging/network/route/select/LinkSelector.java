/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.route.select;


import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.network.route.link.Link;
import io.mapsmessaging.network.route.link.LinkMetrics;
import io.mapsmessaging.network.route.link.LinkState;
import io.mapsmessaging.network.route.link.LinkId;
import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static io.mapsmessaging.logging.ServerLogMessages.EVALUATION_RESULT;
import static io.mapsmessaging.logging.ServerLogMessages.EXCEPTION_DURING_EVALUATION;


/**
 * Periodic evaluator implementing cost-based selection with hysteresis and guardrails.
 */
public class LinkSelector implements Runnable {

  private final Logger logger = LoggerFactory.getLogger(LinkSelector.class);

  @Getter
  private final CostFunction costFunction;
  @Getter
  private final CostWeights costWeights;
  @Getter
  private final SelectionPolicy selectionPolicy;
  @Getter
  private final LinkRepository linkRepository;
  @Getter
  private final LinkSwitcher linkSwitcher;
  private final Deque<Instant> recentSwitchTimes;
  private volatile Instant lastSwitchTime;
  private volatile Instant lastEvaluationTime;

  @Builder
  public LinkSelector(
      CostFunction costFunction,
      CostWeights costWeights,
      SelectionPolicy selectionPolicy,
      LinkRepository linkRepository,
      LinkSwitcher linkSwitcher
  ) {
    this.costFunction = Objects.requireNonNull(costFunction);
    this.costWeights = Objects.requireNonNull(costWeights);
    this.selectionPolicy = Objects.requireNonNull(selectionPolicy);
    this.linkRepository = Objects.requireNonNull(linkRepository);
    this.linkSwitcher = Objects.requireNonNull(linkSwitcher);
    this.recentSwitchTimes = new ArrayDeque<>();
  }

  @Override
  public void run() {
    try {
      SelectionResult selectionResult = evaluateOnce();
      this.lastEvaluationTime = selectionResult.evaluationTime();
      logger.log(EVALUATION_RESULT, selectionResult.toString());
    } catch (Exception exception) {
      logger.log(EXCEPTION_DURING_EVALUATION, exception);
    }
  }

  public SelectionResult evaluateOnce() {
    Instant now = Instant.now();
    Link currentLink = linkSwitcher.getCurrentLink();

    Candidate currentCandidate = score(currentLink);
    java.util.Optional<Candidate> bestOptional = findBestCandidateOptional();
    if (bestOptional.isEmpty()) {
      // No eligible candidates — hold current without throwing
      return new SelectionResult(
          now,
          currentCandidate.link.getLinkId(),
          currentCandidate.cost,
          currentCandidate.link.getLinkId(),
          currentCandidate.cost,
          false,
          "no-eligible-candidates"
      );
    }
    Candidate bestCandidate = bestOptional.get();

    boolean mustFailover = mustFailover(currentCandidate);
    boolean allowSwitchByHysteresis = allowHysteresisSwitch(now, currentCandidate, bestCandidate);


    if (mustFailover || allowSwitchByHysteresis) {
      boolean flapLimited = isFlapLimited(now);
      if (flapLimited && !mustFailover) {
        return new SelectionResult(
            now,
            currentCandidate.link.getLinkId(),
            currentCandidate.cost,
            bestCandidate.link.getLinkId(),
            bestCandidate.cost,
            false,
            "flap-limited"
        );
      }

      boolean switched = linkSwitcher.switchTo(bestCandidate.link, mustFailover ? "hard-failover" : "hysteresis-improvement");
      if (switched) {
        this.lastSwitchTime = now;
        addSwitchTime(now);
        return new SelectionResult(
            now,
            currentCandidate.link.getLinkId(),
            currentCandidate.cost,
            bestCandidate.link.getLinkId(),
            bestCandidate.cost,
            true,
            mustFailover ? "hard-failover" : "switched-for-better-cost"
        );
      } else {
        return new SelectionResult(
            now,
            currentCandidate.link.getLinkId(),
            currentCandidate.cost,
            bestCandidate.link.getLinkId(),
            bestCandidate.cost,
            false,
            "switch-rejected"
        );
      }
    }

    return new SelectionResult(
        now,
        currentCandidate.link.getLinkId(),
        currentCandidate.cost,
        bestCandidate.link.getLinkId(),
        bestCandidate.cost,
        false,
        "hold-current"
    );
  }

  private java.util.Optional<Candidate> findBestCandidateOptional() {
    return linkRepository
        .getAllLinks()
        .stream()
        .filter(this::isLinkEligible)
        .map(this::score)
        .min(java.util.Comparator.comparingDouble(c -> c.cost));
  }


// Inside LinkSelector

  private boolean isLinkEligible(io.mapsmessaging.network.route.link.Link link) {
    if (link == null) return false;

    io.mapsmessaging.network.route.link.LinkState state = link.getState();
    io.mapsmessaging.network.route.link.Link current = linkSwitcher.getCurrentLink();
    boolean isCurrent = current != null && current.getLinkId().equals(link.getLinkId());

    // Current link may be ESTABLISHING or CONNECTED; never FAILED/DISCONNECTED
    if (isCurrent) {
      if (state == io.mapsmessaging.network.route.link.LinkState.FAILED) return false;
      if (state == io.mapsmessaging.network.route.link.LinkState.DISCONNECTED) return false;
      return (state == io.mapsmessaging.network.route.link.LinkState.ESTABLISHING
          || state == io.mapsmessaging.network.route.link.LinkState.CONNECTED);
    }

    // Candidates must be HOLDING or CONNECTED (not CONNECTING/ESTABLISHING/DISCONNECTED/FAILED)
    return (state == io.mapsmessaging.network.route.link.LinkState.HOLDING ||
        state == io.mapsmessaging.network.route.link.LinkState.CONNECTED);
  }


  private Candidate score(Link link) {
    if (link == null) {
      return new Candidate(NullLink.INSTANCE, Double.POSITIVE_INFINITY);
    }
    LinkMetrics linkMetrics = link.getMetrics();
    double base = link.getBaseCost().orElse(0.0);
    double cost = costFunction.compute(linkMetrics, costWeights)+base;
    return new Candidate(link, cost);
  }

// Inside LinkSelector

  private boolean mustFailover(Candidate currentCandidate) {
    Link currentLink = currentCandidate.link;
    if (currentLink == null) return true;

    LinkState state = currentLink.getState();
    if (state == LinkState.FAILED || state == LinkState.DISCONNECTED) return true;
    if (state != LinkState.ESTABLISHING && state != LinkState.CONNECTED) return true;

    LinkMetrics metrics = currentLink.getMetrics();
    if (metrics == null) return false;

    OptionalDouble latency = metrics.getLatencyMillisEma();
    double loss = metrics.getLossRatio();
    double err  = metrics.getErrorRate();

    return (latency.isPresent() && latency.getAsDouble() >= selectionPolicy.getHardMaxLatencyMillis() ||
            loss >= selectionPolicy.getHardMaxLossRatio() ||
            err >= selectionPolicy.getHardMaxErrorRate());
  }



  private boolean allowHysteresisSwitch(Instant now, Candidate currentCandidate, Candidate bestCandidate) {
    if (bestCandidate.link.getLinkId().equals(currentCandidate.link.getLinkId())) return false;
    if (isInCooldown(now)) return false;
    if (!hasHeldLongEnough(now)) return false;

    double hysteresisRatio = selectionPolicy.getHysteresisRatio();
    double epsilon = Math.max(0.0, selectionPolicy.getTieBreakEpsilon());

    double improvementRequired =
        currentCandidate.cost * (1.0 - hysteresisRatio) - Math.abs(currentCandidate.cost) * epsilon;

    return bestCandidate.cost < improvementRequired;
  }
  private boolean hasHeldLongEnough(Instant now) {
    Instant last = this.lastSwitchTime;
    if (last == null) return true;
    Duration elapsed = Duration.between(last, now);
    return elapsed.compareTo(selectionPolicy.getMinimumHoldTime()) >= 0;
  }

  private boolean isInCooldown(Instant now) {
    Instant last = this.lastSwitchTime;
    if (last == null) return false;
    Duration elapsed = Duration.between(last, now);
    return elapsed.compareTo(selectionPolicy.getCooldownAfterSwitch()) < 0;
  }

  private boolean isFlapLimited(Instant now) {
    pruneOldSwitches(now);
    int recentCount = recentSwitchTimes.size();
    return recentCount >= selectionPolicy.getMaxSwitchesPerWindow();
  }

  private void addSwitchTime(Instant now) {
    pruneOldSwitches(now);
    recentSwitchTimes.addLast(now);
  }

  private void pruneOldSwitches(Instant now) {
    Duration window = selectionPolicy.getFlapWindow();
    while (!recentSwitchTimes.isEmpty()) {
      Instant first = recentSwitchTimes.peekFirst();
      if (first.isBefore(now.minus(window))) {
        recentSwitchTimes.removeFirst();
      } else {
        break;
      }
    }
  }

  /**
   * Lightweight placeholder used if current link is null.
   */
  private enum NullLink implements Link {
    INSTANCE;

    @Override
    public LinkId getLinkId() {
      return new LinkId("NULL");
    }

    @Override
    public java.net.URI getRemoteUri() {
      return java.net.URI.create("null://");
    }

    @Override
    public LinkState getState() {
      return LinkState.DISCONNECTED;
    }

    @Override
    public LinkMetrics getMetrics() {
      throw new UnsupportedOperationException("Null link has no metrics");
    }

    @Override
    public OptionalDouble getBaseCost() {
      return OptionalDouble.empty();
    }

    @Override
    public boolean isAvailable() {
      return false;
    }

    @Override
    public void connect() {
    }

    @Override
    public void disconnect() {
    }
  }

  private static final class Candidate {
    private final Link link;
    private final double cost;

    private Candidate(Link link, double cost) {
      this.link = link;
      this.cost = cost;
    }
  }
}
