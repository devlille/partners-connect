(function () {
  
    const navWrapper = document.getElementById('main-nav');
    const navToggle = document.getElementById('toggle-nav');
    
    let navStatus;

    navToggle.addEventListener('click', (e) => {
        navWrapper.classList.toggle('active');
        navStatus = !navStatus;
        e.target.setAttribute('aria-expanded', navStatus);
    });

})();
