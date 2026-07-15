(function () {
    document.addEventListener("click", function (event) {
        var button = event.target.closest("[data-password-toggle]");
        if (!button) {
            return;
        }

        var targetId = button.getAttribute("data-password-toggle-target");
        var input = targetId ? document.getElementById(targetId) : null;
        if (!input) {
            return;
        }

        var shouldShow = input.type === "password";
        input.type = shouldShow ? "text" : "password";
        button.classList.toggle("is-visible", shouldShow);

        var label = shouldShow ? button.dataset.hideLabel : button.dataset.showLabel;
        if (label) {
            button.setAttribute("aria-label", label);
            button.setAttribute("title", label);
            var hiddenLabel = button.querySelector(".visually-hidden");
            if (hiddenLabel) {
                hiddenLabel.textContent = label;
            }
        }
    });
})();
