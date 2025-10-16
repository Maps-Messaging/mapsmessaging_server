#!/usr/bin/env node
import { execSync } from "node:child_process";
import { writeFileSync } from "node:fs";
import { EOL } from "node:os";
import https from "node:https";

/**
 * Release notes: section-per-Jira-issue, misc commits at end.
 * Changes for Matthew:
 *  - Removed Jira "Activity" (changelog) entirely.
 *  - Preserve Jira comments (ADF -> markdown-ish) without escaping < or >.
 */

const args = process.argv.slice(2);
const getArg = (name, fallback = null) => {
  const i = args.indexOf(name);
  return i >= 0 && i + 1 < args.length ? args[i + 1] : fallback;
};
const hasFlag = (name) => args.includes(name);

// Inputs
const branch = getArg("--branch", "development");
const rangeArg = getArg("--range", null);
const sinceTag = hasFlag("--since-tag");
const out = getArg("--out", null);
const issueSections = true; // this file implements the issue-sectioned mode

const jiraEnabled = hasFlag("--jira");
const jiraPrefix = process.env.JIRA_KEY_PREFIX || "MSG";
const jiraBase = process.env.JIRA_BASE || "";
const jiraUser = process.env.JIRA_USER || "";
const jiraToken = process.env.JIRA_TOKEN || "";

const maxComments = parseInt(getArg("--max-comments", "200"), 10);

const nowIso = new Date().toISOString();

// Optional publishers (kept; unused unless flags provided)
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
      .split("\x02")
      .map((s) => s.trim())
      .filter(Boolean)
      .map((line) => {
        const [hash, short, author, date, subject, body] = line.split("\x01");
        return { hash, short, author, date, subject, body: body || "" };
      });
}

function parseConventional(commit) {
  const m = commit.subject.match(/^(\w+)(?:\(([^)]+)\))?(!)?:\s+(.*)$/);
  const type = m ? m[1] : "other";
  const scope = m ? m[2] || null : null;
  const breaking = !!(m && m[3]);
  const summary = m ? m[4] : commit.subject;
  const breakingFooter = /\bBREAKING CHANGE\b/i.test(commit.body);
  return { ...commit, type, scope, breaking: breaking || breakingFooter, summary };
}

function extractJiraKeys(text, prefix) {
  const re = new RegExp(`\\b${prefix}-\\d+\\b`, "g");
  const set = new Set();
  (text.match(re) || []).forEach((k) => set.add(k));
  return Array.from(set);
}

const unique = (arr) => Array.from(new Set(arr));

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
    const qs = `startAt=${startAt}&maxResults=${Math.min(
        maxResults,
        cap - out.length
    )}`;
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
  // Text node
  if (node.type === "text") return node.text || "";
  // Hard break
  if (node.type === "hardBreak") return "\n";
  // Code block
  if (node.type === "codeBlock") {
    const code = (node.content || [])
        .map((n) => (n.type === "text" ? n.text || "" : ""))
        .join("");
    return "```\n" + code + "\n```\n";
  }
  // Paragraph / heading / bulletList / orderedList / listItem / panel — render children sequentially
  const children = (node.content || [])
      .map((n) => adfToMarkdown(n))
      .join("");
  // Put paragraphs on their own line
  if (node.type === "paragraph" || node.type === "heading") {
    return children + "\n\n";
  }
  // Fallback
  return children;
}

function renderAdfDocument(doc) {
  if (!doc) return "";
  if (typeof doc === "string") return doc; // plain text variants
  try {
    return (doc.content || []).map(adfToMarkdown).join("");
  } catch {
    return "";
  }
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
      comments: commentsRaw.map((c) => ({
        author: c.author?.displayName || "unknown",
        updated: c.updated || c.created || "",
        body: renderAdfDocument(c.body), // preserve < and >, no escaping
      })),
    };
  }
  return info;
}

// ---- Formatting
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

function linesForIssueSection(k, info, commits) {
  const i = info[k] || { key: k, url: `${jiraBase}/browse/${k}` };
  const lines = [];
  const title = i.summary
      ? `[${k}](${i.url}) — ${i.summary}`
      : `[${k}](${i.url})`;
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

  if (i.description) {
    lines.push("**Description**");
    // description already includes its own newlines/blocks
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

  if (i.comments && i.comments.length) {
    lines.push("**Comments**");
    for (const c of i.comments) {
      const when = c.updated ? new Date(c.updated).toISOString() : "";
      const body = (c.body || "").trim(); // keep raw, including < and >
      // indent comment content under the bullet
      lines.push(`- ${when} — ${c.author}`);
      // indent multi-line body with two spaces so markdown renders it under the bullet
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
  if (resp.status >= 200 && resp.status < 300)
    console.log("✅ GitHub release created.");
  else
    console.error(
        "❌ GitHub release failed:",
        resp.status,
        (resp.body || "").slice(0, 300)
    );
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
  if (resp.status >= 200 && resp.status < 300)
    console.log("✅ Jira version created.");
  else
    console.error(
        "❌ Jira version create failed:",
        resp.status,
        (resp.body || "").slice(0, 300)
    );
}

// ---- Main
(async function main() {
  const range = determineRange();
  const commits = getCommits(range).map(parseConventional);

  // Map issues and split misc
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

  // Enrich Jira for all referenced keys
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
