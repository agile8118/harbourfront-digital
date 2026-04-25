/**
 * Stress testing POST /api/contact with realistic names, emails, and messages.
 *
 * Example usage:
 *  node scripts/contacts.mjs --url http://localhost:8080 --connections 10 --duration 30 --rate 50
 */

import autocannon from 'autocannon';

const FIRST_NAMES = [
  'James', 'Olivia', 'Liam', 'Emma', 'Noah', 'Ava', 'William', 'Sophia',
  'Benjamin', 'Isabella', 'Lucas', 'Mia', 'Henry', 'Charlotte', 'Alexander',
  'Amelia', 'Mason', 'Harper', 'Ethan', 'Evelyn', 'Daniel', 'Abigail',
  'Michael', 'Emily', 'Matthew', 'Elizabeth', 'Logan', 'Mila', 'Jackson',
  'Ella', 'Sebastian', 'Luna', 'Jack', 'Camila', 'Aiden', 'Penelope',
  'Owen', 'Riley', 'Samuel', 'Zoey', 'Ryan', 'Nora', 'Nathan', 'Lily',
  'Caleb', 'Eleanor', 'Christian', 'Hannah', 'Isaiah', 'Lillian',
];

const LAST_NAMES = [
  'Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller',
  'Davis', 'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez',
  'Wilson', 'Anderson', 'Thomas', 'Taylor', 'Moore', 'Jackson', 'Martin',
  'Lee', 'Perez', 'Thompson', 'White', 'Harris', 'Sanchez', 'Clark',
  'Ramirez', 'Lewis', 'Robinson', 'Walker', 'Young', 'Allen', 'King',
  'Wright', 'Scott', 'Torres', 'Nguyen', 'Hill', 'Flores', 'Green',
  'Adams', 'Nelson', 'Baker', 'Hall', 'Rivera', 'Campbell', 'Mitchell',
  'Carter', 'Roberts',
];

const EMAIL_DOMAINS = [
  'gmail.com', 'yahoo.com', 'outlook.com', 'hotmail.com', 'icloud.com',
  'protonmail.com', 'me.com', 'live.com', 'msn.com', 'aol.com',
  'mail.com', 'zoho.com', 'fastmail.com', 'hey.com', 'tutanota.com',
];

const MESSAGE_PARAGRAPHS = [
  "I came across your website while researching organizations focused on urban cultural development, and I have to say I was genuinely impressed by the breadth of programs you offer. The work you're doing around community engagement and public arts is exactly the kind of initiative our neighbourhood has been hoping to see more of.",
  "We're a small volunteer-run group here in the east end and have been trying to connect with larger institutions that share our values around accessibility and inclusion. It would mean a lot to us to explore whether there might be opportunities for collaboration, even informally at first.",
  "I recently attended one of your public events downtown and left feeling really energized. The atmosphere was welcoming and the programming struck a great balance between being educational and genuinely entertaining. My kids were engaged the whole time, which is no small feat.",
  "I'm reaching out on behalf of a local school board committee that's been exploring partnerships with cultural organizations for our after-school enrichment program. We serve roughly 400 students across three schools and are particularly interested in programs that blend digital literacy with creative expression.",
  "My background is in landscape architecture and I've been following your waterfront revitalization work with a lot of interest. I'd love to find out more about how community members can get involved in the planning process, or whether there are public consultations scheduled for the coming months.",
  "I discovered your organization through a friend who volunteered at one of your festivals last summer. She spoke so highly of the experience that I immediately looked you up. I'd love to find out more about volunteer opportunities, particularly anything involving event coordination or outreach.",
  "As someone who works in municipal government, I'm always looking for examples of effective public-private partnerships around cultural infrastructure. Your model seems to strike a thoughtful balance and I'd be curious to learn more about how funding and programming decisions are made.",
  "I'm a graduate student in urban planning writing my thesis on the role of cultural anchors in neighbourhood revitalization. I'm hoping to include a case study of your organization and would be grateful for the opportunity to speak with someone on your team, even briefly, about your approach and impact metrics.",
  "I run a small independent design studio and we've been looking to partner with nonprofits and civic organizations on pro-bono branding and communications work. If you ever find yourselves in need of visual design support, I'd be happy to have a conversation about how we might help.",
  "I want to commend whoever manages your social media presence — the content has been thoughtful, consistent, and genuinely reflective of the community. It's refreshing to see an organization that communicates with authenticity rather than just posting for the sake of visibility.",
  "We're organizing a neighbourhood street festival this coming summer and are hoping to feature local artists, musicians, and community organizations. We'd love to explore whether there's any interest in having a presence or partnering in some capacity. The event typically draws around 2,000 attendees over the course of the day.",
  "I've been following the waterfront development conversation closely and feel that voices from longtime residents often get lost in those discussions. I'm curious whether your organization plays any advocacy role in those processes or whether you focus primarily on programming and events.",
];

function pick(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function randomName() {
  return `${pick(FIRST_NAMES)} ${pick(LAST_NAMES)}`;
}

function randomEmail(name) {
  const [first, last] = name.toLowerCase().split(' ');
  const patterns = [
    `${first}.${last}`,
    `${first}${last}`,
    `${first[0]}${last}`,
    `${first}.${last}${Math.floor(Math.random() * 90) + 10}`,
    `${first}${Math.floor(Math.random() * 900) + 100}`,
  ];
  return `${pick(patterns)}@${pick(EMAIL_DOMAINS)}`;
}

function randomMessage() {
  const count = Math.floor(Math.random() * 3) + 2; // 2–4 paragraphs
  const shuffled = [...MESSAGE_PARAGRAPHS].sort(() => Math.random() - 0.5);
  return shuffled.slice(0, count).join('\n\n');
}

function buildRequests(pool) {
  return Array.from({ length: pool }, () => {
    const name = randomName();
    return {
      method: 'POST',
      path: '/api/contact',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ name, email: randomEmail(name), message: randomMessage() }),
    };
  });
}

function parseArgs() {
  const args = process.argv.slice(2);
  const opts = {
    url: null,
    connections: 10,
    duration: 30,
    pipelining: 1,
    pool: 500,
    rate: null,
  };

  for (let i = 0; i < args.length; i += 2) {
    const key = args[i]?.replace(/^--/, '');
    const val = args[i + 1];
    if (key === 'url') opts.url = val;
    else if (key === 'connections') opts.connections = parseInt(val, 10);
    else if (key === 'duration') opts.duration = parseInt(val, 10);
    else if (key === 'pipelining') opts.pipelining = parseInt(val, 10);
    else if (key === 'pool') opts.pool = parseInt(val, 10);
    else if (key === 'rate') opts.rate = parseInt(val, 10);
  }

  return opts;
}

async function main() {
  const opts = parseArgs();

  if (!opts.url) {
    console.error('Usage: node scripts/contacts.mjs --url <base-url> [--connections 10] [--duration 30] [--pipelining 1] [--pool 500] [--rate 50]');
    process.exit(1);
  }

  const baseUrl = opts.url.replace(/\/$/, '');
  const requests = buildRequests(opts.pool);

  console.log(`Running: ${opts.connections} connections · ${opts.duration}s · ${opts.pool} pre-generated contact submissions\n`);

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
