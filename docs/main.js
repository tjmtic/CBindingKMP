document.addEventListener('DOMContentLoaded', () => {
    // Scroll animations using Intersection Observer
    const observerOptions = {
        threshold: 0.1,
        rootMargin: '0px 0px -50px 0px'
    };

    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
            }
        });
    }, observerOptions);

    // Track elements to animate
    const animateElements = document.querySelectorAll('.animate-up, .animate-fade-in');
    animateElements.forEach(el => observer.observe(el));

    // Nav blur effect on scroll
    const nav = document.querySelector('.glass-nav');
    window.addEventListener('scroll', () => {
        if (window.scrollY > 50) {
            nav.style.background = 'rgba(5, 10, 20, 0.95)';
            nav.style.height = '70px';
        } else {
            nav.style.background = 'rgba(5, 10, 20, 0.8)';
            nav.style.height = '80px';
        }
    });

    // Add smooth hover effect to feature cards
    const cards = document.querySelectorAll('.feature-card');
    cards.forEach(card => {
        card.addEventListener('mousemove', (e) => {
            const rect = card.getBoundingClientRect();
            const x = e.clientX - rect.left;
            const y = e.clientY - rect.top;
            
            card.style.setProperty('--mouse-x', `${x}px`);
            card.style.setProperty('--mouse-y', `${y}px`);
        });
    });
});
