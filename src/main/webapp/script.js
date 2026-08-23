const CLIENT_URL = '/api/clients';
const MATERIAL_URL = '/api/materials';
const QUOTE_URL = '/api/quotations';

function showMessage(text, isError = false) {
    const box = document.getElementById('messageBox');
    box.textContent = text;
    box.className = isError ? 'error' : 'success';
    box.style.display = 'block';
    setTimeout(() => { box.style.display = 'none'; }, 4000);
}

async function loadData() {
    try {
        // Load Clients for Dropdown
        const clientRes = await fetch(CLIENT_URL);
        const clients = await clientRes.json();
        const select = document.getElementById('quoteClientSelect');
        select.innerHTML = '<option value="">Choose Client...</option>';
        clients.forEach(c => {
            const opt = document.createElement('option');
            opt.value = c.id;
            opt.textContent = `${c.clientName} (${c.contactNumber || 'No #'})`;
            select.appendChild(opt);
        });

        // Load Quotations Table
        const quoteRes = await fetch(QUOTE_URL);
        const quotes = await quoteRes.json();
        const tbody = document.getElementById('quotationTableBody');
        tbody.innerHTML = '';
        quotes.forEach(q => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${q.id}</td>
                <td>${q.clientName}</td>
                <td>${q.projectTitle}</td>
                <td>₱${Number(q.laborCost).toFixed(2)}</td>
                <td style="font-weight: bold; color: #047857;">₱${Number(q.totalAmount).toFixed(2)}</td>
                <td>${q.status}</td>
            `;
            tbody.appendChild(row);
        });
    } catch (err) {
        console.error('Failed to load dashboard data');
    }
}

// Client Form Submit
document.getElementById('clientForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const clientName = document.getElementById('clientName').value;
    const contactNumber = document.getElementById('contactNumber').value;
    const address = document.getElementById('address').value;

    const res = await fetch(CLIENT_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clientName, contactNumber, address })
    });

    if (res.ok) {
        showMessage('Client saved successfully!');
        document.getElementById('clientForm').reset();
        loadData();
    } else {
        showMessage('Failed to save client.', true);
    }
});

// Material Form Submit
document.getElementById('materialForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const materialName = document.getElementById('materialName').value;
    const unitCost = Number(document.getElementById('unitCost').value);

    const res = await fetch(MATERIAL_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ materialName, unitCost })
    });

    if (res.ok) {
        showMessage('Material added to inventory!');
        document.getElementById('materialForm').reset();
    } else {
        showMessage('Failed to add material.', true);
    }
});

// Quotation Form Submit
document.getElementById('quotationForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const clientId = Number(document.getElementById('quoteClientSelect').value);
    const projectTitle = document.getElementById('projectTitle').value;
    const laborCost = Number(document.getElementById('laborCost').value);

    const res = await fetch(QUOTE_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ clientId, projectTitle, laborCost })
    });

    if (res.ok) {
        showMessage('Quotation generated successfully!');
        document.getElementById('quotationForm').reset();
        loadData();
    } else {
        showMessage('Failed to generate quotation.', true);
    }
});

loadData();