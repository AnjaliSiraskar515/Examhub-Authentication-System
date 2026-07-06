let inboxMessages = [];
let activeMessageId = null;

document.addEventListener('DOMContentLoaded', () => {
    // Expose functions globally for HTML onclick handlers
    window.showCommMessage = showCommMessage;
    window.replyToMessage = replyToMessage;
    window.requestReplyPermission = requestReplyPermission;
    window.acknowledgeMessage = acknowledgeMessage;
    window.deleteMessage = deleteMessage;
    window.deleteMessage = deleteMessage;
    window.loadInboxMessages = loadInboxMessages;
    window.filterInboxMessages = filterInboxMessages;
    window.openComposeHeadSupervisorModal = openComposeHeadSupervisorModal;
    window.closeComposeHeadSupervisorModal = closeComposeHeadSupervisorModal;
    window.submitComposeHeadSupervisor = submitComposeHeadSupervisor;
    window.toggleBlockSupervisor = toggleBlockSupervisor;

    // Attach listener for send reply button
    const btnSendReply = document.getElementById('btn-send-reply');
    if (btnSendReply) {
        btnSendReply.addEventListener('click', sendReply);
    }

    // Load inbox in background to update unread badge on page load
    setTimeout(() => {
        if (typeof authFetch === 'function') {
            loadInboxMessages();
        }
    }, 500);
});

async function loadInboxMessages() {
    const inboxList = document.getElementById('inbox-list');
    if (!inboxList) return;

    inboxList.innerHTML = '<div class="p-4 text-center text-gray-500"><i class="fas fa-spinner fa-spin mr-2"></i>Loading messages...</div>';

    try {
        const res = await authfetch((window.GLOBAL_API_BASE || '') + '/api/university/communication/inbox');
        if (!res.ok) throw new Error('Failed to fetch messages');
        
        const data = await res.json();
        if (data.success) {
            inboxMessages = data.data || [];
            renderInbox();
        } else {
            throw new Error(data.message || 'Error fetching messages');
        }
    } catch (err) {
        console.error(err);
        inboxList.innerHTML = `<div class="p-4 text-center text-red-500">${err.message}</div>`;
    }
}

function renderInbox() {
    const inboxList = document.getElementById('inbox-list');
    if (!inboxList) return;

    const filterVal = document.getElementById('inbox-filter') ? document.getElementById('inbox-filter').value : 'ALL';
    const filtered = filterVal === 'ALL' ? inboxMessages : inboxMessages.filter(m => m.senderRole === filterVal);

    if (filtered.length === 0) {
        inboxList.innerHTML = '<div class="p-8 text-center text-gray-400">No messages in inbox.</div>';
        return;
    }

    let html = '';
    filtered.forEach(msg => {
        let typeClass = 'bg-gray-100 text-gray-600';
        if (msg.type === 'WARNING') typeClass = 'bg-red-100 text-red-600';
        else if (msg.type === 'BROADCAST') typeClass = 'bg-blue-100 text-blue-600';
        else if (msg.type === 'GENERAL') typeClass = 'bg-green-100 text-green-600';

        const dateStr = new Date(msg.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
        const unreadClass = msg.status === 'SENT' ? 'font-extrabold text-gray-900 dark:text-white' : 'font-semibold text-gray-800 dark:text-gray-200';
        const unreadDot = msg.status === 'SENT' ? '<span class="w-2 h-2 rounded-full bg-blue-500 inline-block mr-1"></span>' : '';

        const senderLabel = msg.senderRole === 'SUPER_ADMIN' ? 'Super Admin' : 'Head Supervisor';
        html += `
            <div class="p-4 border-b border-gray-50 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer ${msg.status === 'SENT' ? 'bg-blue-50/30 dark:bg-gray-700/50' : ''}" onclick="showCommMessage(${msg.id})">
                <div class="flex justify-between items-start mb-1">
                    <span class="text-xs font-bold ${typeClass} px-2 py-0.5 rounded">${msg.type || 'GENERAL'}</span>
                    <span class="text-[10px] text-gray-400">${dateStr}</span>
                </div>
                <h4 class="${unreadClass} text-sm mt-2">${unreadDot}${msg.subject}</h4>
                <p class="text-xs text-gray-500 mt-1 truncate">${msg.senderDetails ? msg.senderDetails : msg.senderEmail} • ${msg.message || ''}</p>
            </div>
        `;
    });

    inboxList.innerHTML = html;
    updateUnreadBadge();
}

function updateUnreadBadge() {
    const badge = document.getElementById('comm-unread-badge');
    if (!badge) return;
    const unreadCount = inboxMessages.filter(m => m.status === 'SENT').length;
    if (unreadCount > 0) {
        badge.innerText = unreadCount;
        badge.classList.remove('hidden');
    } else {
        badge.classList.add('hidden');
    }
}

function filterInboxMessages() {
    renderInbox();
}

async function showCommMessage(msgId) {
    const msg = inboxMessages.find(m => m.id === msgId);
    if (!msg) return;

    activeMessageId = msgId;

    document.getElementById('comm-empty-state').classList.add('hidden');
    const view = document.getElementById('comm-msg-view');
    view.classList.remove('hidden');
    
    document.getElementById('comm-msg-subject').innerText = msg.subject;
    const dateStr = new Date(msg.createdAt).toLocaleString('en-US', { month: 'long', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit' });
    document.getElementById('comm-msg-date').innerText = dateStr;

    // Update the sender label dynamically based on actual senderRole
    const senderEl = document.getElementById('comm-msg-sender');
    if (senderEl) {
        if (msg.senderRole === 'UNIVERSITY_ADMIN') {
            senderEl.innerHTML = `<i class="fas fa-paper-plane mr-1 text-green-500"></i> Sent to: ${msg.receiverEmail || 'Head Supervisor'}`;
        } else if (msg.senderRole === 'HEAD_SUPERVISOR') {
            const senderText = msg.senderDetails ? msg.senderDetails : 'Head Supervisor';
            senderEl.innerHTML = `<i class="fas fa-user-tie mr-1 text-indigo-500"></i> From: ${senderText}`;
        } else {
            senderEl.innerHTML = '<i class="fas fa-user-shield mr-1 text-blue-500"></i> From: Super Admin';
        }
    }
    
    let typeClass = 'bg-gray-100 text-gray-700 border-gray-200';
    if (msg.type === 'WARNING') typeClass = 'bg-red-100 text-red-700 border-red-200';
    else if (msg.type === 'BROADCAST') typeClass = 'bg-blue-100 text-blue-700 border-blue-200';
    else if (msg.type === 'GENERAL') typeClass = 'bg-green-100 text-green-700 border-green-200';

    const typeBadge = document.getElementById('comm-msg-type');
    typeBadge.innerText = msg.type || 'GENERAL';
    typeBadge.className = `text-xs font-bold px-3 py-1 rounded border ${typeClass}`;
    
    document.getElementById('comm-msg-body').innerText = msg.message;

    // Render actions based on status, type, and sender role
    const actionsContainer = document.getElementById('comm-actions');
    let actionsHtml = '';
    const isFromSuperAdmin = msg.senderRole === 'SUPER_ADMIN';
    const isFromHeadSupervisor = msg.senderRole === 'HEAD_SUPERVISOR';

    const isFromUniversityAdmin = msg.senderRole === 'UNIVERSITY_ADMIN';

    if (isFromUniversityAdmin) {
        // Message sent BY the admin: just show the current status and delete button
        const statusClass = msg.status === 'READ' ? 'text-green-600 bg-green-100' : 'text-blue-600 bg-blue-100';
        const statusText = msg.status === 'READ' ? 'Read by Recipient' : 'Delivered';
        const statusIcon = msg.status === 'READ' ? 'fa-check-double' : 'fa-check';
        actionsHtml += `<span class="text-xs font-bold ${statusClass} px-3 py-1.5 rounded-lg mr-2"><i class="fas ${statusIcon} mr-1"></i> ${statusText}</span>`;
    } else if (isFromSuperAdmin) {
        // Super Admin messages: WARNING needs acknowledgement; GENERAL uses reply permission workflow
        if (msg.type === 'WARNING' && !msg.acknowledged) {
            actionsHtml += `<button onclick="acknowledgeMessage(${msg.id})" class="px-4 py-2 bg-red-600 hover:bg-red-700 text-white font-medium rounded-lg text-sm transition-colors shadow-md mr-2"><i class="fas fa-check-double mr-1"></i> Acknowledge Warning</button>`;
        } else if (msg.type === 'WARNING' && msg.acknowledged) {
            actionsHtml += `<span class="text-xs font-bold text-green-600 bg-green-100 px-3 py-1.5 rounded-lg mr-2"><i class="fas fa-check mr-1"></i> Acknowledged</span>`;
        }

        if (msg.type === 'GENERAL' || !msg.type) {
            if (msg.status === 'APPROVED' && msg.replyAllowed) {
                actionsHtml += `<button onclick="replyToMessage()" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg text-sm transition-colors shadow-md mr-2"><i class="fas fa-reply mr-1"></i> Reply</button>`;
            } else if (msg.status === 'REQUESTED') {
                actionsHtml += `<span class="text-xs font-bold text-yellow-600 bg-yellow-100 px-3 py-1.5 rounded-lg mr-2"><i class="fas fa-hourglass-half mr-1"></i> Reply Requested...</span>`;
            } else {
                actionsHtml += `<button onclick="requestReplyPermission(${msg.id})" class="px-4 py-2 bg-white border border-gray-300 hover:bg-gray-50 text-gray-700 font-medium rounded-lg text-sm transition-colors mr-2"><i class="fas fa-hand-paper mr-1"></i> Request Reply Permission</button>`;
            }
        }
    } else if (isFromHeadSupervisor) {
        // Head Supervisor messages: direct reply always allowed (no permission needed)
        actionsHtml += `<button onclick="replyToMessage()" class="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-medium rounded-lg text-sm transition-colors shadow-md mr-2"><i class="fas fa-reply mr-1"></i> Reply</button>`;
        actionsHtml += `<button onclick="toggleBlockSupervisor('${msg.senderEmail}', true)" class="px-3 py-2 bg-red-50 text-red-700 hover:bg-red-100 border border-red-200 rounded-lg text-sm transition-colors mr-2"><i class="fas fa-ban mr-1"></i> Block Head Supervisor</button>`;
        actionsHtml += `<button onclick="toggleBlockSupervisor('${msg.senderEmail}', false)" class="px-3 py-2 bg-green-50 text-green-700 hover:bg-green-100 border border-green-200 rounded-lg text-sm transition-colors mr-2"><i class="fas fa-check-circle mr-1"></i> Unblock</button>`;
    }

    actionsHtml += `<button onclick="deleteMessage(${msg.id})" class="px-3 py-2 bg-red-50 text-red-600 hover:bg-red-100 rounded-lg text-sm transition-colors"><i class="fas fa-trash"></i></button>`;

    actionsContainer.innerHTML = actionsHtml;
    document.getElementById('reply-container').classList.add('hidden');
    document.getElementById('reply-message').value = '';

    // Mark as read if it is SENT, and NOT sent by us
    if (msg.status === 'SENT' && !isFromUniversityAdmin) {
        try {
            const res = await authfetch((window.GLOBAL_API_BASE || '') + '/api/university/communication/mark-read', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ id: msg.id })
            });
            if (res.ok) {
                msg.status = 'READ';
                renderInbox(); // Update bold text in sidebar
            }
        } catch (e) {
            console.error('Failed to mark read', e);
        }
    }
}

function replyToMessage() {
    document.getElementById('reply-container').classList.remove('hidden');
    document.getElementById('reply-message').focus();
}

async function requestReplyPermission(msgId) {
    try {
        const res = await authfetch((window.GLOBAL_API_BASE || '') + '/api/messages/request-reply', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: msgId })
        });
        const data = await res.json();
        if (data.success) {
            alert('Reply permission requested successfully.');
            // Update local state
            const msg = inboxMessages.find(m => m.id === msgId);
            if (msg) msg.status = 'REQUESTED';
            showCommMessage(msgId);
        } else {
            alert('Failed: ' + data.message);
        }
    } catch (e) {
        alert('Error requesting reply permission.');
    }
}

async function acknowledgeMessage(msgId) {
    try {
        const res = await authfetch((window.GLOBAL_API_BASE || '') + '/api/university/communication/acknowledge', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ id: msgId })
        });
        const data = await res.json();
        if (data.success) {
            alert('Warning acknowledged.');
            const msg = inboxMessages.find(m => m.id === msgId);
            if (msg) {
                msg.acknowledged = true;
                msg.status = 'READ';
            }
            renderInbox();
            showCommMessage(msgId);
        } else {
            alert('Failed: ' + data.message);
        }
    } catch (e) {
        alert('Error acknowledging message.');
    }
}

async function sendReply() {
    if (!activeMessageId) return;
    const text = document.getElementById('reply-message').value.trim();
    if (!text) {
        alert('Please enter a message to reply.');
        return;
    }

    const btn = document.getElementById('btn-send-reply');
    btn.disabled = true;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...';

    try {
        const res = await authfetch((window.GLOBAL_API_BASE || '') + '/api/messages/reply', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ parentMessageId: activeMessageId, message: text })
        });
        const data = await res.json();
        if (data.success) {
            alert('Reply sent successfully!');
            document.getElementById('reply-container').classList.add('hidden');
            document.getElementById('reply-message').value = '';
        } else {
            alert('Failed to send reply: ' + data.message);
        }
    } catch (e) {
        alert('Error sending reply.');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Send Reply';
    }
}

async function deleteMessage(msgId) {
    if (!confirm('Are you sure you want to delete this message?')) return;

    try {
        const res = await authFetch(`/api/university/communication/${msgId}`, {
            method: 'DELETE'
        });
        const data = await res.json();
        if (data.success) {
            inboxMessages = inboxMessages.filter(m => m.id !== msgId);
            document.getElementById('comm-msg-view').classList.add('hidden');
            document.getElementById('comm-empty-state').classList.remove('hidden');
            renderInbox();
        } else {
            alert('Failed to delete: ' + data.message);
        }
    } catch (e) {
        alert('Error deleting message.');
    }
}

async function openComposeHeadSupervisorModal() {
    document.getElementById('compose-head-modal').classList.remove('hidden');
    document.getElementById('compose-head-modal').classList.add('flex');
    const select = document.getElementById('compose-head-recipient');
    select.innerHTML = '<option value="">Loading Head Supervisors...</option>';
    
    try {
        const res = await authfetch((window.GLOBAL_API_BASE || '') + '/api/university/head-supervisors');
        const data = await res.json();
        if (data.success && data.data) {
            select.innerHTML = '';
            if (data.data.length === 0) {
                select.innerHTML = '<option value="">No Head Supervisors found</option>';
            }
            data.data.forEach(hs => {
                const collegeName = (hs.college && hs.college.name) ? hs.college.name : (hs.collegeName || hs.department || 'No College');
                select.innerHTML += `<option value="${hs.email}">${hs.name} — ${collegeName}</option>`;
            });

        }
    } catch (e) {
        select.innerHTML = '<option value="">Error loading</option>';
    }
}

function closeComposeHeadSupervisorModal() {
    document.getElementById('compose-head-modal').classList.add('hidden');
    document.getElementById('compose-head-modal').classList.remove('flex');
    document.getElementById('compose-head-subject').value = '';
    document.getElementById('compose-head-body').value = '';
}

async function submitComposeHeadSupervisor() {
    const recipientEmail = document.getElementById('compose-head-recipient').value;
    const subject = document.getElementById('compose-head-subject').value;
    const body = document.getElementById('compose-head-body').value;
    const priority = document.getElementById('compose-head-priority').value;

    if (!recipientEmail || !subject || !body) {
        alert("Please select recipient and fill in subject/message");
        return;
    }

    try {
        const res = await authfetch((window.GLOBAL_API_BASE || '') + '/api/university/communication/send', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                recipientEmail,
                subject,
                message: body,
                priority,
                type: 'GENERAL'
            })
        });
        const data = await res.json();
        if (data.success) {
            alert("Message sent successfully!");
            closeComposeHeadSupervisorModal();
            loadInboxMessages();
        } else {
            alert("Failed to send: " + data.message);
        }
    } catch (e) {
        alert("Error sending message.");
    }
}

async function toggleBlockSupervisor(email, block) {
    if (!confirm(`Are you sure you want to ${block ? 'BLOCK' : 'UNBLOCK'} this supervisor from messaging you?`)) return;

    try {
        const res = await authfetch((window.GLOBAL_API_BASE || '') + '/api/university/communication/block-supervisor', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, block: block.toString() })
        });
        const data = await res.json();
        if (data.success) {
            alert(`Supervisor ${block ? 'blocked' : 'unblocked'} successfully.`);
        } else {
            alert("Failed: " + data.message);
        }
    } catch (e) {
        alert("Error toggling block state.");
    }
}
