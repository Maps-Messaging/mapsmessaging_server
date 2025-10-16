\
  #!/usr/bin/env node
  import { execSync } from 'node:child_process';
  import { writeFileSync } from 'node:fs';
  import { EOL } from 'node:os';
  import https from 'node:https';

  const args = process.argv.slice(2);
  const getArg = (name, fallback=null) => {
    const i = args.indexOf(name);
    if (i >= 0 && i+1 < args.length) return args[i+1];
    return fallback;
  };
  const hasFlag = (name) => args.includes(name);

  const branch = getArg('--branch', 'development');
  const rangeArg = getArg('--range', null);
  const sinceTag = hasFlag('--since-tag');
  const out = getArg('--out', null);
  const jiraEnabled = hasFlag('--jira');
  const jiraPrefix = process.env.JIRA_KEY_PREFIX || 'MSG';
  const jiraBase = process.env.JIRA_BASE || '';
  const jiraUser = process.env.JIRA_USER || '';
  const jiraToken = process.env.JIRA_TOKEN || '';
  const nowIso = new Date().toISOString();

  // Optional publishers
  const ghToken = process.env.GITHUB_TOKEN || '';
  const ghRepo = getArg('--repo', null); // owner/name
  const ghTag  = getArg('--tag', null);
  const doGithubRelease = hasFlag('--github-release');
  const jiraVersionName  = getArg('--jira-version', null);

  function run(cmd) { return execSync(cmd, { encoding: 'utf8' }).trim(); }

  function determineRange() {
    if (rangeArg) return rangeArg;
    if (sinceTag) {
      try {
        const lastTag = run(`git describe --tags --abbrev=0 ${branch}`);
        return `${lastTag}..${branch}`;
      } catch {
        const root = run('git rev-list --max-parents=0 HEAD');
        return `${root}..${branch}`;
      }
    }
    const base = run(`git merge-base main ${branch}`);
    return `${base}..${branch}`;
  }

  function getCommits(range) {
    const format = ['%H','%h','%an','%ad','%s','%b'].join('%x01');
    const raw = run(`git log --no-merges --date=short --pretty=format:'${format}%x02' ${range}`);
    const entries = raw.split('\x02').map(s => s.trim()).filter(Boolean);
    return entries.map(line => {
      const [hash, short, author, date, subject, body] = line.split('\x01');
      return { hash, short, author, date, subject, body: body || '' };
    });
  }

  function parseConventional(commit) {
    const m = commit.subject.match(/^(\w+)(?:\(([^)]+)\))?(!)?:\s+(.*)$/);
    const type = m ? m[1] : 'other';
    const scope = m ? (m[2] || null) : null;
    const breaking = !!(m && m[3]);
    const summary = m ? m[4] : commit.subject;
    const breakingFooter = /\bBREAKING CHANGE\b/i.test(commit.body);
    return { ...commit, type, scope, breaking: breaking || breakingFooter, summary };
  }

  function extractJiraKeys(text, prefix) {
    const re = new RegExp(`\\b${prefix}-\\d+\\b`, 'g');
    const set = new Set();
    (text.match(re) || []).forEach(k => set.add(k));
    return Array.from(set);
  }
  const unique = arr => Array.from(new Set(arr));

  function groupByType(commits) {
    const groups = new Map();
    for (const c of commits) {
      const key = c.type || 'other';
      if (!groups.has(key)) groups.set(key, []);
      groups.get(key).push(c);
    }
    return groups;
  }

  // HTTPS helpers
  function jiraGet(path) {
    return new Promise((resolve, reject) => {
      const url = new URL(`${jiraBase}${path}`);
      const auth = Buffer.from(`${jiraUser}:${jiraToken}`).toString('base64');
      const opts = { method: 'GET', headers: { 'Authorization': `Basic ${auth}`, 'Accept': 'application/json' } };
      const req = https.request(url, opts, (res) => {
        let data=''; res.on('data', d => data += d);
        res.on('end', () => {
          if (res.statusCode >= 200 && res.statusCode < 300) {
            try { resolve(JSON.parse(data)); } catch { resolve(null); }
          } else resolve(null);
        });
      });
      req.on('error', reject); req.end();
    });
  }

  function httpPostJson(urlStr, body, headers={}) {
    return new Promise((resolve, reject) => {
      const url = new URL(urlStr);
      const data = JSON.stringify(body);
      const opts = { method: 'POST', headers: { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(data), ...headers } };
      const req = https.request(url, opts, (res) => {
        let buf=''; res.on('data', d => buf += d);
        res.on('end', () => resolve({status: res.statusCode, body: buf}));
      });
      req.on('error', reject);
      req.write(data); req.end();
    });
  }

  async function enrichJira(keys) {
    const result = {};
    if (!jiraEnabled || !jiraBase || !jiraUser || !jiraToken) return result;
    for (const k of keys) {
      const json = await jiraGet(`/rest/api/3/issue/${k}`);
      if (json) {
        result[k] = { key: k, summary: json.fields?.summary || '', status: json.fields?.status?.name || '', url: `${jiraBase}/browse/${k}` };
      } else result[k] = { key: k };
    }
    return result;
  }

  function formatMarkdown(title, commits, jiraInfo, range) {
    const groups = groupByType(commits);
    const order = ['feat','fix','perf','refactor','docs','test','build','ci','chore','other'];
    const lines = [];
    lines.push(`# ${title}`);
    lines.push(`_Generated: ${nowIso}_`);
    lines.push('');
    lines.push(`**Range**: \`${range}\``);
    lines.push('');

    const highlights = commits.filter(c => c.type === 'feat' || c.type === 'fix' || c.breaking).slice(0, 5);
    if (highlights.length) {
      lines.push('## Highlights');
      for (const c of highlights) {
        const keys = extractJiraKeys(`${c.summary} ${c.body}`, jiraPrefix);
        const keyStr = keys.length ? ` (${keys.join(', ')})` : '';
        lines.push(`- ${c.summary}${keyStr} — \`${c.short}\` by ${c.author}`);
      }
      lines.push('');
    }

    for (const t of order) {
      const items = groups.get(t) || [];
      if (!items.length) continue;
      const titleCase = t[0].toUpperCase() + t.slice(1);
      lines.push(`## ${titleCase}`);
      for (const c of items) {
        const keys = extractJiraKeys(`${c.summary} ${c.body}`, jiraPrefix);
        const keyStr = keys.length ? ` (${keys.map(k => jiraInfo[k]?.url ? `[${k}](${jiraInfo[k].url})` : k).join(', ')})` : '';
        const breaking = c.breaking ? ' **(BREAKING)**' : '';
        lines.push(`- ${c.summary}${breaking}${keyStr} — \`${c.short}\` by ${c.author}`);
      }
      lines.push('');
    }

    const allKeys = unique(commits.flatMap(c => extractJiraKeys(`${c.summary} ${c.body}`, jiraPrefix)));
    if (allKeys.length) {
      lines.push('## Linked Jira Issues');
      for (const k of allKeys) {
        const info = jiraInfo[k];
        if (info?.summary) lines.push(`- [${k}](${info.url}) — ${info.summary} (${info.status})`);
        else lines.push(`- ${k}`);
      }
      lines.push('');
    }

    return lines.join(EOL);
  }

  async function publishGithubRelease(tag, name, markdown, repo) {
    if (!doGithubRelease) return;
    if (!ghToken || !tag || !repo) { console.error('GitHub release skipped: missing token/tag/repo'); return; }
    const url = `https://api.github.com/repos/${repo}/releases`;
    const headers = { 'Authorization': `Bearer ${ghToken}`, 'User-Agent': 'maps-release-notes-tool', 'Accept': 'application/vnd.github+json' };
    const payload = { tag_name: tag, name: name || tag, body: markdown, draft: false, prerelease: false };
    const resp = await httpPostJson(url, payload, headers);
    if (resp.status >= 200 && resp.status < 300) console.log('GitHub release created.');
    else console.error('GitHub release failed:', resp.status, (resp.body||'').slice(0,500));
  }

  async function createJiraVersionIfNeeded(name) {
    if (!jiraEnabled || !jiraBase || !jiraUser || !jiraToken) return;
    if (!name) return;
    const projectId = process.env.JIRA_PROJECT_ID || null;
    if (!projectId) { console.error('Jira version skipped: set JIRA_PROJECT_ID'); return; }
    const url = `${jiraBase}/rest/api/3/version`;
    const auth = Buffer.from(`${jiraUser}:${jiraToken}`).toString('base64');
    const headers = { 'Authorization': `Basic ${auth}` };
    const payload = { name, projectId, released: false };
    const resp = await httpPostJson(url, payload, headers);
    if (resp.status >= 200 && resp.status < 300) console.log('Jira version created.');
    else console.error('Jira version create failed:', resp.status, (resp.body||'').slice(0,500));
  }

  (async function main() {
    const range = determineRange();
    const commits = getCommits(range).map(parseConventional).sort((a,b)=> a.date.localeCompare(b.date));
    const allKeys = unique(commits.flatMap(c => extractJiraKeys(`${c.summary} ${c.body}`, jiraPrefix)));
    const jiraInfo = await enrichJira(allKeys);
    const title = getArg('--title', `Release Notes for ${branch}`);
    const md = formatMarkdown(title, commits, jiraInfo, range);

    if (out) { writeFileSync(out, md, 'utf8'); console.log(`Wrote ${out}`); }
    else { process.stdout.write(md + EOL); }

    // Optional publishers
    await publishGithubRelease(ghTag, title, md, ghRepo);
    await createJiraVersionIfNeeded(jiraVersionName);
  })().catch(err => { console.error(err); process.exit(1); });
