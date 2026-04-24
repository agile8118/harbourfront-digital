// Sticky header
const header = document.getElementById('header');
window.addEventListener('scroll', () => {
  header.classList.toggle('scrolled', window.scrollY > 20);
});

// Mobile nav toggle
const menuToggle = document.getElementById('menuToggle');
const nav = document.querySelector('nav');
menuToggle.addEventListener('click', () => {
  nav.classList.toggle('open');
});
document.addEventListener('click', (e) => {
  if (!nav.contains(e.target) && !menuToggle.contains(e.target)) {
    nav.classList.remove('open');
  }
});

// Card reveal on scroll
const cards = document.querySelectorAll('.card');
const observer = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      const card = entry.target;
      const delay = card.dataset.index * 120;
      setTimeout(() => card.classList.add('visible'), delay);
      observer.unobserve(card);
    }
  });
}, { threshold: 0.15 });
cards.forEach((card) => observer.observe(card));

// Animated stat counters
function animateCount(el, target, duration = 1600) {
  const start = performance.now();
  const update = (now) => {
    const progress = Math.min((now - start) / duration, 1);
    const eased = 1 - Math.pow(1 - progress, 3);
    el.textContent = Math.floor(eased * target);
    if (progress < 1) requestAnimationFrame(update);
    else el.textContent = target;
  };
  requestAnimationFrame(update);
}

const statNums = document.querySelectorAll('.stat-num');
const statsObserver = new IntersectionObserver((entries) => {
  entries.forEach((entry) => {
    if (entry.isIntersecting) {
      const el = entry.target;
      animateCount(el, parseInt(el.dataset.target, 10));
      statsObserver.unobserve(el);
    }
  });
}, { threshold: 0.5 });
statNums.forEach((el) => statsObserver.observe(el));

// Newsletter form
const newsletterForm = document.getElementById('newsletterForm');
const newsletterSuccess = document.getElementById('newsletterSuccess');

newsletterForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  const emailInput = document.getElementById('newsletterEmail');
  const emailError = document.getElementById('newsletterEmailError');
  const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

  if (!emailRe.test(emailInput.value.trim())) {
    emailError.textContent = 'Enter a valid email address.';
    return;
  }
  emailError.textContent = '';

  const controls = newsletterForm.querySelectorAll('input, button');
  controls.forEach((el) => (el.disabled = true));

  try {
    const res = await fetch('/api/newsletter', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: emailInput.value.trim() }),
    });

    if (res.ok) {
      emailInput.value = '';
      newsletterSuccess.classList.add('show');
    } else {
      controls.forEach((el) => (el.disabled = false));
      alert('Something went wrong. Please try again.');
    }
  } catch {
    controls.forEach((el) => (el.disabled = false));
    alert('Network error. Please try again.');
  }
});

// Contact form validation
const form = document.getElementById('contactForm');
const formSuccess = document.getElementById('formSuccess');

function validate() {
  let valid = true;

  const name = document.getElementById('name');
  const nameError = document.getElementById('nameError');
  if (!name.value.trim()) {
    nameError.textContent = 'Name is required.';
    valid = false;
  } else {
    nameError.textContent = '';
  }

  const email = document.getElementById('email');
  const emailError = document.getElementById('emailError');
  const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  if (!emailRe.test(email.value.trim())) {
    emailError.textContent = 'Enter a valid email address.';
    valid = false;
  } else {
    emailError.textContent = '';
  }

  const message = document.getElementById('message');
  const messageError = document.getElementById('messageError');
  if (message.value.trim().length < 10) {
    messageError.textContent = 'Message must be at least 10 characters.';
    valid = false;
  } else {
    messageError.textContent = '';
  }

  return valid;
}

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!validate()) return;

  const inputs = form.querySelectorAll('input, textarea, button');
  inputs.forEach((el) => (el.disabled = true));

  try {
    const res = await fetch('/api/contact', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name: document.getElementById('name').value.trim(),
        email: document.getElementById('email').value.trim(),
        message: document.getElementById('message').value.trim(),
      }),
    });

    if (res.ok) {
      document.getElementById('name').value = '';
      document.getElementById('email').value = '';
      document.getElementById('message').value = '';
      inputs.forEach((el) => (el.disabled = false));
      formSuccess.classList.add('show');
    } else {
      inputs.forEach((el) => (el.disabled = false));
      alert('Something went wrong. Please try again.');
    }
  } catch {
    inputs.forEach((el) => (el.disabled = false));
    alert('Network error. Please try again.');
  }
});
