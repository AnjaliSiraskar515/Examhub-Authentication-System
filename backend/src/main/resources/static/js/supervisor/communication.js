// Communication Hub Logic for Head Supervisor Dashboard

let headCurrentMessage = null;

// Removed auto-fetch on page load to prevent 500 errors when tab is inactive.
// Initialization now relies exclusively on tab clicks.

// Initialize when tab is opened
window.loadHeadSupervisorInbox = async function() {
    const listContainer = document.getElementById('head-inbox-list');
    if (!listContainer) return;
    
    listContainer.innerHTML = '<div class="text-center p-6 text-gray-400 text-sm"><i class="fas fa-spinner fa-spin mr-2"></i>Loading messages...</div>';
    
    try {
        const res = await fetch('/api/head-supervisor/communication/inbox', {
            headers: authHeaders()
        }).catch(() => null);
        
        let messages = [];
        
        if (res && res.ok) {
            const data = await res.json().catch(() => ({}));
            if (data && data.success) {
                messages = data.data || [];
            }
        }
        
        renderHeadInbox(messages);
        
    } catch (err) {
        // Fallback
        renderHeadInbox([]);
    }
}

window.deleteMessageSupervisor = deleteMessageSupervisor;
window.requestReplyPermissionSupervisor = requestReplyPermissionSupervisor;
window.viewHeadMessage = viewHeadMessage;

function renderHeadInbox(messages) {
    const listContainer = document.getElementById('head-inbox-list');
    listContainer.innerHTML = '';
    
    // Update Badge
    const badge = document.getElementById('comm-unread-badge');
    if (badge) {
        const unreadCount = messages.filter(m => m.status === 'SENT' && m.receiverRole === 'HEAD_SUPERVISOR').length;
        if (unreadCount > 0) {
            badge.innerText = unreadCount;
            badge.classList.remove('hidden');
        } else {
            badge.classList.add('hidden');
        }
    }
    
    if (messages.length === 0) {
        listContainer.innerHTML = `
            <div class="text-center p-8 text-gray-400">
                <i class="fas fa-inbox text-4xl mb-3 opacity-30"></i>
                <p class="text-sm">Inbox is empty</p>
            </div>
        `;
        return;
    }
    
    messages.forEach(msg => {
        const isUnread = msg.status === 'SENT' && msg.receiverRole === 'HEAD_SUPERVISOR';
        const isSentByMe = msg.senderRole === 'HEAD_SUPERVISOR';
        
        const div = document.createElement('div');
        div.className = `p-3 rounded-xl border cursor-pointer transition-all hover:bg-indigo-50/50 ${isUnread ? 'bg-indigo-50/30 border-indigo-200' : 'bg-white border-gray-100 hover:border-indigo-200'}`;
        div.onclick = () => viewHeadMessage(msg, div);
        
        const dateStr = new Date(msg.createdAt).toLocaleDateString();
        const typeBadgeClass = msg.type === 'WARNING' ? 'bg-red-100 text-red-800' : 
                              (msg.type === 'BROADCAST' ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800');
        
        div.innerHTML = `
            <div class="flex justify-between items-start mb-1">
                <span class="text-[10px] font-bold px-2 py-0.5 rounded uppercase tracking-wider ${typeBadgeClass}">
                    ${msg.type || 'GENERAL'}
                </span>
                <span class="text-xs text-gray-400">${dateStr}</span>
            </div>
            <h4 class="text-sm font-semibold text-gray-800 truncate pr-4 ${isUnread ? 'font-bold' : ''}">${msg.subject}</h4>
            <p class="text-xs text-gray-500 mt-1 truncate">${isSentByMe ? 'To: Univ Admin' : 'From: Univ Admin'} • ${msg.message}</p>
        `;
        
        listContainer.appendChild(div);
    });
}

async function viewHeadMessage(msg, element) {
    headCurrentMessage = msg;
    
    // UI selection state
    document.querySelectorAll('#head-inbox-list > div').forEach(el => {
        el.classList.remove('border-indigo-400', 'ring-1', 'ring-indigo-400');
    });
    if (element) {
        element.classList.add('border-indigo-400', 'ring-1', 'ring-indigo-400');
    }
    
    // Toggle views
    document.getElementById('head-msg-empty-state').classList.add('hidden');
    document.getElementById('head-msg-view-area').classList.remove('hidden');
    
    // Populate details
    document.getElementById('head-msg-subject').innerText = msg.subject;
    const isSentByMe = msg.senderRole === 'HEAD_SUPERVISOR';
    document.getElementById('head-msg-meta').innerText = isSentByMe ? `Sent to: University Admin (${msg.receiverEmail}) • ${new Date(msg.createdAt).toLocaleString()}` : `From: University Admin (${msg.senderEmail}) • ${new Date(msg.createdAt).toLocaleString()}`;
    
    const typeBadge = document.getElementById('head-msg-type-badge');
    typeBadge.innerText = msg.type || 'GENERAL';
    typeBadge.className = 'px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider ' + 
        (msg.type === 'WARNING' ? 'bg-red-100 text-red-800' : 
        (msg.type === 'BROADCAST' ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800'));
        
    document.getElementById('head-msg-body').innerText = msg.message;
    
    // Render actions
    const actionsContainer = document.getElementById('head-msg-actions');
    let actionsHtml = '';
    
    if (isSentByMe) {
        // Just show delivery status
        const statusClass = msg.status === 'READ' ? 'text-green-600 bg-green-100' : 'text-blue-600 bg-blue-100';
        const statusText = msg.status === 'READ' ? 'Read' : 'Delivered';
        actionsHtml += `<span class="text-[10px] font-bold ${statusClass} px-2 py-1 rounded"><i class="fas fa-check-double mr-1"></i> ${statusText}</span>`;
    } else {
        // Received message: can request reply if it's GENERAL and not already allowed
        if (msg.type === 'GENERAL' || !msg.type) {
            if (msg.status === 'APPROVED' && msg.replyAllowed) {
                actionsHtml += `<span class="text-[10px] font-bold text-green-600 bg-green-100 px-2 py-1 rounded"><i class="fas fa-reply mr-1"></i> Reply Allowed</span>`;
            } else if (msg.status === 'REQUESTED') {
                actionsHtml += `<span class="text-[10px] font-bold text-yellow-600 bg-yellow-100 px-2 py-1 rounded"><i class="fas fa-clock mr-1"></i> Requested</span>`;
            } else {
                actionsHtml += `<button onclick="requestReplyPermissionSupervisor(${msg.id})" class="text-[10px] bg-white border border-gray-200 hover:bg-gray-50 text-gray-600 font-bold px-2 py-1 rounded transition-colors"><i class="fas fa-hand-paper mr-1"></i> Request Reply</button>`;
            }
        }
    }
    
    // Always show delete button
    actionsHtml += `<button onclick="deleteMessageSupervisor(${msg.id})" class="text-[10px] bg-red-50 text-red-600 hover:bg-red-100 px-2 py-1 rounded transition-colors ml-auto"><i class="fas fa-trash"></i></button>`;
    
    actionsContainer.innerHTML = actionsHtml;

    // Prepare compose area for reply if it's from Admin
    if (!isSentByMe) {
        document.getElementById('head-compose-title').innerText = "Reply to Admin";
        document.getElementById('head-compose-subject').value = "Re: " + msg.subject;
        
        // Hide compose area if reply is not allowed (request workflow)
        if ((msg.type === 'GENERAL' || !msg.type) && !msg.replyAllowed) {
            // Option to show it anyway or hide it. Let's keep it but maybe dim it? 
            // Better to match university admin logic: only allow reply if status is APPROVED or if it's WARNING/BROADCAST
        }
    } else {
        document.getElementById('head-compose-title').innerText = "Send to University Admin";
        document.getElementById('head-compose-subject').value = "";
    }
    
    // Mark as read if received and SENT
    if (!isSentByMe && msg.status === 'SENT') {
        try {
            fetch('/api/head-supervisor/communication/mark-read', {
                method: 'POST',
                headers: authHeaders({ 'Content-Type': 'application/json' }),
                body: JSON.stringify({ id: msg.id })
            }).then(res => {
                if (res.ok) {
                    msg.status = 'READ';
                    if (element) {
                        const h4 = element.querySelector('h4');
                        if (h4) h4.classList.remove('font-bold');
                        element.classList.remove('bg-indigo-50/30', 'border-indigo-200');
                        element.classList.add('bg-white', 'border-gray-100');
                    }
                    const badge = document.getElementById('comm-unread-badge');
                    if (badge) {
                        let count = parseInt(badge.innerText || '0');
                        if (count > 0) {
                            count--;
                            badge.innerText = count;
                            if (count === 0) badge.classList.add('hidden');
                        }
                    }
                }
            });
        } catch (e) {}
    }
}

async function deleteMessageSupervisor(id) {
    if (!confirm('Delete this message?')) return;
    try {
        const res = await fetch(`/api/head-supervisor/communication/${id}`, { 
            method: 'DELETE',
            headers: authHeaders()
        });
        const data = await res.json();
        if (data.success) {
            document.getElementById('head-msg-view-area').classList.add('hidden');
            document.getElementById('head-msg-empty-state').classList.remove('hidden');
            loadHeadSupervisorInbox();
        } else {
            alert(data.message);
        }
    } catch (e) {
        alert('Error deleting message');
    }
}

async function requestReplyPermissionSupervisor(id) {
    try {
        const res = await fetch('/api/messages/request-reply', {
            method: 'POST',
            headers: authHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({ id: id })
        });
        const data = await res.json();
        if (data.success) {
            alert('Reply permission requested!');
            loadHeadSupervisorInbox();
        } else {
            alert(data.message);
        }
    } catch (e) {
        alert('Error requesting permission');
    }
}

window.sendHeadSupervisorMessage = async function() {
    const subject = document.getElementById('head-compose-subject').value;
    const body = document.getElementById('head-compose-body').value;
    const priority = document.getElementById('head-compose-priority').value;
    
    if (!subject || !body) {
        showToast("Please enter subject and message", "error");
        return;
    }
    
    const btn = event.target.closest('button');
    const oldHtml = btn.innerHTML;
    btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Sending...';
    btn.disabled = true;
    
    try {
        // Map dropdown selection to backend type and priority fields
        const selectedVal = document.getElementById('head-compose-priority').value;
        const msgType = selectedVal === 'URGENT' ? 'WARNING' : 'GENERAL';
        const msgPriority = selectedVal === 'URGENT' ? 'URGENT' : 'NORMAL';

        const res = await fetch('/api/head-supervisor/communication/send', {
            method: 'POST',
            headers: authHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({
                subject: subject,
                message: body,
                priority: msgPriority,
                type: msgType
            })
        });
        
        const data = await res.json();
        if (data.success) {
            showToast("Message sent to University Admin", "success");
            document.getElementById('head-compose-subject').value = '';
            document.getElementById('head-compose-body').value = '';
            loadHeadSupervisorInbox();
        } else {
            showToast(data.message || "Failed to send message", "error");
        }
    } catch (err) {
        console.error(err);
        showToast("Error sending message", "error");
    } finally {
        btn.innerHTML = oldHtml;
        btn.disabled = false;
    }
}

// Ensure the tab trigger works
const originalShowTab = window.showTab;
window.showTab = function(tabName) {
    if (originalShowTab) originalShowTab(tabName);
    if (tabName === 'communication') {
        loadHeadSupervisorInbox();
    }
}
