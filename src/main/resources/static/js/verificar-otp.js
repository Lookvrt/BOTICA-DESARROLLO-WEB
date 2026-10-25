// Auto-avanza entre cajas y compila el código al enviar
const boxes = document.querySelectorAll('.otp-box');
const hidden = document.getElementById('codigo-hidden');
const form   = document.getElementById('otp-form');

boxes.forEach((box, i) => {
    box.addEventListener('input', () => {
        box.value = box.value.replace(/[^0-9]/g, '');
        if (box.value && i < boxes.length - 1) boxes[i + 1].focus();
    });
    box.addEventListener('keydown', (e) => {
        if (e.key === 'Backspace' && !box.value && i > 0) boxes[i - 1].focus();
    });
    // Pegar código de 6 dígitos de una vez
    box.addEventListener('paste', (e) => {
        e.preventDefault();
        const pasted = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, 6);
        boxes.forEach((b, idx) => { b.value = pasted[idx] || ''; });
        boxes[Math.min(pasted.length, 5)].focus();
    });
});

form.addEventListener('submit', (e) => {
    const code = Array.from(boxes).map(b => b.value).join('');
    if (code.length !== 6) {
        e.preventDefault();
        alert('Ingresa los 6 dígitos del código.');
        return;
    }
    hidden.value = code;
});