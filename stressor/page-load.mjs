/**
 * Stress testing page loads: GET / → GET /styles.css → GET /main.js
 *
 * Example usage:
 *  node scripts/page-load.mjs --url http://localhost:8080 --connections 10 --duration 30 --rate 50
 */

import autocannon from 'autocannon';

function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    url: null,
    connections: 10,
    duration: 30,
    pipelining: 1,
    rate: null,
  };

  for (let i = 0; i < args.length; i += 2) {
    const key = args[i]?.replace(/^--/, '');
    const val = args[i + 1];
    if (key === 'url') opts.url = val;
    else if (key === 'connections') opts.connections = parseInt(val, 10);
    else if (key === 'duration') opts.duration = parseInt(val, 10);
    else if (key === 'pipelining') opts.pipelining = parseInt(val, 10);
    else if (key === 'rate') opts.rate = parseInt(val, 10);
  }

  return opts;
}

async function main() {
  const opts = parseArgs();

  if (!opts.url) {
    console.error('Usage: node scripts/page-load.mjs --url <base-url> [--connections 10] [--duration 30] [--pipelining 1] [--rate 50]');
    process.exit(1);
  }

  const baseUrl = opts.url.replace(/\/$/, '');

  const requests = [
    { method: 'GET', path: '/' },
    { method: 'GET', path: '/styles.css' },
    { method: 'GET', path: '/main.js' },
  ];

  console.log(`Running: ${opts.connections} connections · ${opts.duration}s · page load pipeline (/, /styles.css, /main.js)\n`);

  const instance = autocannon(
    {
      url: baseUrl,
      connections: opts.connections,
      duration: opts.duration,
      pipelining: opts.pipelining,
      ...(opts.rate && { overallRate: opts.rate }),
      requests,
    },
    (err, result) => {
      if (err) { console.error('Autocannon error:', err); process.exit(1); }
      console.log('\nStatus codes:', result.statusCodeStats);
    },
  );

  autocannon.track(instance);
}

main().catch(err => { console.error(err); process.exit(1); });
