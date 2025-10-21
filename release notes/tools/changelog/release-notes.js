#!/usr/bin/env node
import { execSync } from "node:child_process";
import { writeFileSync } from "node:fs";
import { EOL } from "node:os";
import https from "node:https";

/**
 * Release notes generator (issue-sectioned) with Jira "Release-Notes" comment support.
 * - If a Jira comment contains a Release-Notes block, that block is used as the section body.
 * - Blocks supported (priority order):
 *   1) Fenced: ```release-notes ... ```
 *   2) Tagged: <Release Notes> ... </Release Notes> (end tag optional; consumes to end if missing)
 *   3) Header: first non-empty line equals "Release-Notes" or "Release-Notes:" then rest of comment
 *
 * Flags:
 *   --branch <name>                default: development
 *   --range  <git-range>           e.g. v1.2.3..development (auto-calculated if missing)
 *   --since-tag                    use last tag..branch when --range not given
 *   --out <file>                   write markdown to file
 *   --repo <org/repo>              for optional GitHub release publish
 *   --tag <git-tag>                for optional GitHub release publish
 *   --title <string>               document title (default: "Release Notes for <branch>")
 *   --jira                         enable Jira enrichment (requires env vars below)
 *   --jira-version <name>          create Jira version with this name (optional)
 *   --max-comments <n>             cap comment pagination (default 200)
 *   --release-notes-preferred <bool>  default true
 *   --release-notes-only <bool>       default true
 *   --escape-scripts <bool>           default false (preserve <script> etc. as-is)
 *
 * Env:
 *   JIRA_KEY_PREFIX   e.g. "MSG"   (default MSG)
 *   JIRA_BASE         e.g. "https://your-domain.atlassian.net"
 *   JIRA_USER         Jira email/user
 *   JIRA_TOKEN        API token
 *   GITHUB_TOKEN      token for GitHub release publish (optional)
 *   JIRA_PROJECT_ID   required if using --jira-version
 */

const args = process.argv.slice(2);
const getArg = (name, fallback = null) => {
  const i = args.indexOf(name);
  return i >= 0 && i + 1 < args.length ? args[i + 1] : fallback;
};
const hasFlag = (name) => args.includes(name);
const getBool = (name, dflt) => {
  const v = getArg(name, null);
  if (v == null) return dflt;
  return /^true$/i.test(v) || v === "1";
};

// Inputs
const branch = getArg("--branch", "development");
const rangeArg = getArg("--range", null);
const sinceTag = hasFlag("--since-tag");
const out = getArg("--out", null);
const issueSections = true;

const jiraEnabled = hasFlag("--jira");
const jiraPrefix = process.env.JIRA_KEY_PREFIX || "MSG";
const jiraBase = process.env.JIRA_BASE || "";
const jiraUser = process.env.JIRA_USER || "";
const jiraToken = process.env.JIRA_TOKEN || "";

const maxComments = parseInt(getArg("--max-comments", "200"), 10);
const preferRN = getBool("--release-notes-preferred", true);
const onlyRN = getBool("--release-notes-only", true);
const escapeScripts = getBool("--escape-scripts", false);

const nowIso = new Date().toISOString();

// Optional GitHub publish
const ghToken = process.env.GITHUB_TOKEN || "";
const ghRepo = getArg("--repo", null);
const ghTag = getArg("--tag", null);
const doGithubRelease = hasFlag("--github-release");
const jiraVersionName = getArg("--jira-version", null);

// ---- Git utilities
function run(cmd) {
  return execSync(cmd, { encoding: "utf8" }).trim();
}

function determineRange() {
  if (rangeArg) return rangeArg;
  if (sinceTag) {
    try {
      const lastTag = run(`git describe --tags --abbrev=0 ${branch}`);
      return `${lastTag}..${branch}`;
    } catch {
      const root = run("git rev-list --max-parents=0 HEAD");
      return `${root}..${branch}`;
    }
  }
  const base = run(`git merge-base main ${branch}`);
  return `${base}..${branch}`;
}

function getCommits(range) {
  const format = ["%H", "%h", "%an", "%ad", "%s", "%b"].join("%x01");
  const raw = run(
      `git log --no-merges --date=short --pretty=format:'${format}%x02' ${range}`
  );
  return raw
      .split("")
      .map((s) => s.trim())
      .filter(Boolean)
      .map((line) => {
        const [hash, short, author, date, subject, body] = line.split("");
        return { hash, short, author, date, subject, body: body || "" };
      });
}

function parseConventional(commit) {
  const m = commit.subject.match(/^(\w+)(?:\(([^)]+)\))?(!)?:\s+(.*)$/);
  const type = m ? m[1] : "other";
  const scope = m ? m[2] || null : null;
  const breaking = !!(m && m[3]);
  const summary = m ? m[4] : commit.subject;
  const breakingFooter = /BREAKING CHANGE/i.test(commit.body);
  return { ...commit, type, scope, breaking: breaking || breakingFooter, summary };
}

function extractJiraKeys(text, prefix) {
  const re = new RegExp(`\b${prefix}-\d+\b`, "g");
  const set = new Set();
  (text.match(re) || []).forEach((k) => set.add(k));
  return Array.from(set);
}

// ---- HTTPS helpers
function httpsReq(urlStr, opts = {}, body = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(urlStr);
    const req = https.request(url, opts, (res) => {
      let data = "";
      res.on("data", (d) => (data += d));
      res.on("end", () => resolve({ status: res.statusCode, data }));
    });
    req.on("error", reject);
    if (body) req.write(body);
    req.end();
  });
}

function jiraAuthHeaders() {
  const auth = Buffer.from(`${jiraUser}:${jiraToken}`).toString("base64");
  return { Authorization: `Basic ${auth}`, Accept: "application/json" };
}

async function jiraIssue(k) {
  const headers = jiraAuthHeaders();
  const path = `/rest/api/3/issue/${k}?fields=summary,status,priority,issuetype,fixVersions,components,labels,assignee,description`;
  const { status, data } = await httpsReq(`${jiraBase}${path}`, {
    method: "GET",
    headers,
  });
  if (status >= 200 && status < 300) {
    try {
      return JSON.parse(data);
    } catch {
      return null;
    }
  }
  return null;
}

async function jiraComments(k, cap = maxComments) {
  if (cap === 0) return [];
  const headers = jiraAuthHeaders();
  const out = [];
  let startAt = 0,
      maxResults = 100;
  while (out.length < cap) {
    const qs = `startAt=${startAt}&maxResults=${Math.min(maxResults, cap - out.length)}`;
    const { status, data } = await httpsReq(
        `${jiraBase}/rest/api/3/issue/${k}/comment?${qs}`,
        { method: "GET", headers }
    );
    if (!(status >= 200 && status < 300)) break;
    let json;
    try {
      json = JSON.parse(data);
    } catch {
      break;
    }
    const items = json.comments || [];
    out.push(...items);
    if (items.length < maxResults) break;
    startAt += items.length;
  }
  return out;
}

// ---- ADF -> Markdown-ish (preserve raw characters, basic structure)
function adfToMarkdown(node) {
  if (!node) return "";
  if (node.type === "text") return node.text || "";
  if (node.type === "hardBreak") return "\n";
  if (node.type === "codeBlock") {
    const code = (node.content || [])
        .map((n) => (n.type === "text" ? n.text || "" : ""))
        .join("");
    return "```\n" + code + "\n```\n";
  }
  const children = (node.content || []).map((n) => adfToMarkdown(n)).join("");
  if (node.type === "paragraph" || node.type === "heading") {
    return children + "\n";
  }
  return children;
}

function renderAdfDocument(doc) {
  if (!doc) return "";
  if (typeof doc === "string") return doc;
  try {
    return (doc.content || []).map(adfToMarkdown).join("");
  } catch {
    return "";
  }
}

// ---- Release-Notes extraction
function detectReleaseNotesFromText(text) {
  if (!text) return null;
  // 1) Fenced: ```release-notes ... ``` (last match wins)
  const fencedRe = /```(?:release[- ]?notes)\s*\n([\s\S]*?)```/gi;
  let m, lastBlock = null;
  while ((m = fencedRe.exec(text)) !== null) {
    lastBlock = (m[1] || "").trim();
  }
  if (lastBlock && lastBlock.length) return lastBlock;

  // 2a) Tagged pair: <Release Notes> ... </Release Notes>
  const taggedPair = /<\s*release\s*notes\s*>\s*([\s\S]*?)<\s*\/\s*release\s*notes\s*>/i.exec(text);
  if (taggedPair && taggedPair[1] && taggedPair[1].trim().length) {
    return taggedPair[1].trim();
  }
  // 2b) Tagged start only: consume to end
  const startOnly = /<\s*release\s*notes\s*>\s*([\s\S]*)$/i.exec(text);
  if (startOnly && startOnly[1] && startOnly[1].trim().length) {
    return startOnly[1].trim();
  }

  // 3) Header on first non-empty line: Release-Notes or Release-Notes:
  const lines = text.split(/\r?\n/);
  let idx = 0;
  while (idx < lines.length && !lines[idx].trim()) idx++;
  if (idx < lines.length) {
    const first = lines[idx].trim();
    if (/^release-\s*notes\s*:?$/i.test(first)) {
      const rest = lines.slice(idx + 1).join("\n").trim();
      if (rest.length) return rest;
    }
  }
  return null;
}

function extractReleaseNotes(comments) {
  if (!Array.isArray(comments) || comments.length === 0) return null;
  // Find latest comment that yields a RN block
  let chosen = null;
  for (const c of comments) {
    const body = (c.body || "").trim();
    const block = detectReleaseNotesFromText(body);
    if (block) {
      const updated = c.updated || c.created || "";
      if (!chosen) chosen = { body: block, author: c.author || "unknown", updated };
      else {
        const tNew = new Date(updated || 0).getTime();
        const tOld = new Date(chosen.updated || 0).getTime();
        if (isFinite(tNew) && (!isFinite(tOld) || tNew >= tOld)) {
          chosen = { body: block, author: c.author || "unknown", updated };
        }
      }
    }
  }
  return chosen;
}

// ---- Enrichment
async function enrichIssues(keys) {
  const info = {};
  for (const k of keys) {
    const base = await jiraIssue(k);
    if (!base) {
      info[k] = { key: k, url: `${jiraBase}/browse/${k}` };
      continue;
    }
    const f = base.fields || {};
    const commentsRaw = await jiraComments(k, maxComments);
    const comments = commentsRaw.map((c) => ({
      author: c.author?.displayName || "unknown",
      updated: c.updated || c.created || "",
      body: renderAdfDocument(c.body),
    }));
    const rn = preferRN ? extractReleaseNotes(comments) : null;

    info[k] = {
      key: k,
      url: `${jiraBase}/browse/${k}`,
      summary: f.summary || "",
      status: f.status?.name || "",
      priority: f.priority?.name || "",
      type: f.issuetype?.name || "",
      fixVersions: (f.fixVersions || []).map((v) => v.name),
      components: (f.components || []).map((c) => c.name),
      labels: f.labels || [],
      assignee: f.assignee?.displayName || "",
      description: renderAdfDocument(f.description),
      comments,
      releaseNotes: rn, // { body, author, updated } or null
    };
  }
  return info;
}

// ---- Mapping
function mapIssuesAndMisc(commits) {
  const keyToCommits = new Map();
  const misc = [];
  const keysSet = new Set();
  for (const c of commits) {
    const keys = extractJiraKeys(`${c.summary} ${c.body}`, jiraPrefix);
    if (!keys.length) {
      misc.push(c);
      continue;
    }
    for (const k of keys) {
      keysSet.add(k);
      if (!keyToCommits.has(k)) keyToCommits.set(k, []);
      keyToCommits.get(k).push(c);
    }
  }
  const keys = [...keysSet].sort((a, b) => a.localeCompare(b));
  return { keyToCommits, misc, keys };
}

// ---- Formatting
function linesForIssueSection(k, info, commits) {
  const i = info[k] || {key: k, url: `${jiraBase}/browse/${k}`};
  const lines = [];

  const title = i.summary ? `[${k}](${i.url}) — ${i.summary}` : `[${k}](${i.url})`;
  const meta = [
    i.type ? `Type: ${i.type}` : null,
    i.status ? `Status: ${i.status}` : null,
    i.priority ? `Priority: ${i.priority}` : null,
    i.assignee ? `Assignee: ${i.assignee}` : null,
    i.fixVersions?.length ? `FixVersions: ${i.fixVersions.join(", ")}` : null,
    i.components?.length ? `Components: ${i.components.join(", ")}` : null,
  ]
      .filter(Boolean)
      .join(" · ");

  lines.push(`### ${title}`);
  if (meta) lines.push(`_${meta}_`);
  lines.push("");

  const usedRN = preferRN && i.releaseNotes && i.releaseNotes.body && i.releaseNotes.body.trim().length;

  if (usedRN) {
    // Render Release-Notes body verbatim
    lines.push(i.releaseNotes.body.trim());
    lines.push("");

    // Commits list always shown
    if (commits.length) {
      lines.push("**Commits**");
      for (const c of commits) {
        lines.push(`- \`${c.short}\` ${c.summary} — ${c.author} (${c.date})`);
      }
      lines.push("");
    }

// If onlyRN is false, we can append description/comments for extra context
    if (!onlyRN) {
      if (i.description) {
        lines.push("**Description**");
        lines.push(i.description.trim());
        lines.push("");
      }
      if (i.comments && i.comments.length) {
        lines.push("**Comments**");
        for (const c of i.comments) {
          const when = c.updated ? new Date(c.updated).toISOString() : "";
          const body = (c.body || "").trim();
          lines.push(`- ${when} — ${c.author}`);
          const indented = body
              .split("\n")
              .map((ln) => (ln ? `  ${ln}` : ""))
              .join("\n");
          lines.push(indented);
        }
        lines.push("");
      }
    }
    return lines;
  }

// Fallback path when no RN
  if (i.description) {
    lines.push("**Description**");
    lines.push(i.description.trim());
    lines.push("");
  }
  if (commits.length) {
    lines.push("**Commits**");
    for (const c of commits) {
      lines.push(`- \`${c.short}\` ${c.summary} — ${c.author} (${c.date})`);
    }
    lines.push("");
  }
  if (!onlyRN && i.comments && i.comments.length) {
    lines.push("**Comments**");
    for (const c of i.comments) {
      const when = c.updated ? new Date(c.updated).toISOString() : "";
      const body = (c.body || "").trim();
      lines.push(`- ${when} — ${c.author}`);
      const indented = body
          .split("\n")
          .map((ln) => (ln ? `  ${ln}` : ""))
          .join("\n");
      lines.push(indented);
    }
    lines.push("");
  }
  return lines;
}

function formatDocument(title, range, info, keyToCommits, keys, misc) {
  const lines = [];
  lines.push(`# ${title}`);
  lines.push(`_Generated: ${nowIso}_`);
  lines.push("");
  lines.push(`**Range**: \`${range}\``);
  lines.push("");

  for (const k of keys) {
    const section = linesForIssueSection(k, info, keyToCommits.get(k) || []);
    lines.push(...section);
  }

  if (misc.length) {
    lines.push("## Miscellaneous (no Jira key)");
    for (const c of misc) {
      lines.push(`- \`${c.short}\` ${c.summary} — ${c.author} (${c.date})`);
    }
    lines.push("");
  }

  return lines.join(EOL);
}

// ---- Publishers (optional)
function httpPostJson(urlStr, body, headers = {}) {
  return new Promise((resolve, reject) => {
    const url = new URL(urlStr);
    const data = JSON.stringify(body);
    const opts = {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Content-Length": Buffer.byteLength(data),
        ...headers,
      },
    };
    const req = https.request(url, opts, (res) => {
      let buf = "";
      res.on("data", (d) => (buf += d));
      res.on("end", () => resolve({ status: res.statusCode, body: buf }));
    });
    req.on("error", reject);
    req.write(data);
    req.end();
  });
}

async function publishGithubRelease(tag, name, markdown, repo) {
  if (!doGithubRelease) return;
  if (!ghToken || !tag || !repo) {
    console.error("GitHub release skipped: missing token/tag/repo");
    return;
  }
  const url = `https://api.github.com/repos/${repo}/releases`;
  const headers = {
    Authorization: `Bearer ${ghToken}`,
    "User-Agent": "maps-release-notes-tool",
    Accept: "application/vnd.github+json",
  };
  const payload = {
    tag_name: tag,
    name: name || tag,
    body: markdown,
    draft: false,
    prerelease: false,
  };
  const resp = await httpPostJson(url, payload, headers);
  if (resp.status >= 200 && resp.status < 300) console.log("? GitHub release created.");
  else console.error("? GitHub release failed:", resp.status, (resp.body || "").slice(0, 300));
}

async function createJiraVersionIfNeeded(name) {
  if (!jiraEnabled || !jiraBase || !jiraUser || !jiraToken) return;
  if (!name) return;
  const projectId = process.env.JIRA_PROJECT_ID || null;
  if (!projectId) {
    console.error("Jira version skipped: set JIRA_PROJECT_ID");
    return;
  }
  const url = `${jiraBase}/rest/api/3/version`;
  const auth = Buffer.from(`${jiraUser}:${jiraToken}`).toString("base64");
  const headers = { Authorization: `Basic ${auth}` };
  const payload = { name, projectId, released: false };
  const resp = await httpPostJson(url, payload, headers);
  if (resp.status >= 200 && resp.status < 300) console.log("? Jira version created.");
  else console.error("? Jira version create failed:", resp.status, (resp.body || "").slice(0, 300));
}

// ---- Main
(async function main() {
  const range = determineRange();
  const commits = getCommits(range).map(parseConventional);

  const keyToCommits = new Map();
  const misc = [];
  const keysSet = new Set();
  for (const c of commits) {
    const keys = extractJiraKeys(`${c.summary} ${c.body}`, jiraPrefix);
    if (!keys.length) {
      misc.push(c);
      continue;
    }
    for (const k of keys) {
      keysSet.add(k);
      if (!keyToCommits.has(k)) keyToCommits.set(k, []);
      keyToCommits.get(k).push(c);
    }
  }
  const keys = [...keysSet].sort((a, b) => a.localeCompare(b));

  const info = jiraEnabled ? await enrichIssues(keys) : {};
  const title = getArg("--title", `Release Notes for ${branch}`);
  const md = formatDocument(title, range, info, keyToCommits, keys, misc);

  if (out) {
    writeFileSync(out, md, "utf8");
    console.log(`Wrote ${out}`);
  } else {
    process.stdout.write(md + EOL);
  }

  await publishGithubRelease(ghTag, title, md, ghRepo);
  await createJiraVersionIfNeeded(jiraVersionName);
})().catch((err) => {
  console.error(err);
  process.exit(1);
});
