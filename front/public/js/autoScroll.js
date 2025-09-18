/*(function () {

  const allScrollPanels = document.querySelectorAll('.auto-scroll');
  const getHeight = function(panel) {
    let windowH = window.innerHeight;
    let panelTop = panel.getBoundingClientRect().top;
    let panelHeight = windowH - panelTop;

    return panelHeight;
  }

  if (allScrollPanels.length > 0) {    
    allScrollPanels.forEach((p) => {
      if(window.innerWidth >= 1000) {
         p.style.height = getHeight(p) - 50 + "px";
      } else {
        p.style.height = "auto";
      }
    });
  }

  window.addEventListener('resize', function(event) {
    allScrollPanels.forEach((p) => {
      if(window.innerWidth >= 1000) {
        p.style.height = getHeight(p) - 50 + "px";
      } else {
        p.style.height = "auto";
      }
    });
  }, true);

})();*/
