/**
 * FlowTrack AI Chatbot Widget
 * 
 * Icon is visible on ALL pages. When opened:
 *  - Not logged in       → "Please log in" message
 *  - RETAILER, no plan   → premium gate / upgrade prompt
 *  - RETAILER, subscribed → full chat (retailer context)
 *  - PLATFORM_ADMIN       → full chat (admin context)
 * 
 * Context is injected server-side via /api/chatbot/status so the
 * AI response is always scoped to the correct role / business.
 */
class ChatbotWidget {
    constructor() {
        this.isOpen   = false;
        this.status   = null; // loaded from /api/chatbot/status
        this.loading  = true;
        this.init();
    }

    // ── Bootstrap ────────────────────────────────────────────────────
    async init() {
        this.createWidget();
        this.attachEventListeners();

        // Fetch subscription / role status once on page load
        try {
            const res = await fetch('/api/chatbot/status');
            if (res.ok) {
                this.status = await res.json();
            }
        } catch (_) {
            this.status = null;
        }
        this.loading = false;
    }

    // ── DOM Creation ─────────────────────────────────────────────────
    createWidget() {
        const html = `
        <div class="ft-chat-wrap" id="ftChatWrap">

            <!-- Floating toggle button – ALWAYS visible -->
            <button class="ft-toggle" id="ftToggle" title="FlowTrack AI Assistant">
                <i class="fas fa-robot"></i>
                <span class="ft-badge" id="ftBadge"></span>
            </button>

            <!-- Chat window (hidden until toggle clicked) -->
            <div class="ft-window" id="ftWindow">

                <!-- Header -->
                <div class="ft-header">
                    <div class="ft-header-info">
                        <div class="ft-avatar"><i class="fas fa-robot"></i></div>
                        <div>
                            <div class="ft-title">FlowTrack AI</div>
                            <div class="ft-subtitle" id="ftSubtitle">Assistant</div>
                        </div>
                    </div>
                    <button class="ft-close" id="ftClose">
                        <i class="fas fa-times"></i>
                    </button>
                </div>

                <!-- Body – swapped between states below -->
                <div class="ft-body" id="ftBody">
                    <!-- Populated dynamically -->
                </div>

            </div>
        </div>`;
        document.body.insertAdjacentHTML('beforeend', html);
    }

    // ── Events ───────────────────────────────────────────────────────
    attachEventListeners() {
        document.getElementById('ftToggle').addEventListener('click', () => this.toggle());
        document.getElementById('ftClose').addEventListener('click',  () => this.toggle());
    }

    toggle() {
        this.isOpen = !this.isOpen;
        const win    = document.getElementById('ftWindow');
        const toggle = document.getElementById('ftToggle');

        if (this.isOpen) {
            win.classList.add('active');
            toggle.innerHTML = '<i class="fas fa-times"></i>';
            this.renderBody();  // decide which UI to show
        } else {
            win.classList.remove('active');
            toggle.innerHTML = '<i class="fas fa-robot"></i>';
        }
    }

    // ── Body renderer (gate ↔ chat) ──────────────────────────────────
    renderBody() {
        const body = document.getElementById('ftBody');

        // Still fetching status
        if (this.loading) {
            body.innerHTML = `<div class="ft-gate">
                <div class="ft-gate-icon"><i class="fas fa-spinner fa-spin"></i></div>
                <p>Loading…</p>
            </div>`;
            return;
        }

        const s = this.status;

        // ① Not logged in
        if (!s || !s.loggedIn) {
            body.innerHTML = `<div class="ft-gate">
                <div class="ft-gate-icon lock"><i class="fas fa-lock"></i></div>
                <h3>Login Required</h3>
                <p>Please sign in to your FlowTrack account to use the AI Assistant.</p>
                <a href="/login" class="ft-gate-btn">Sign In</a>
            </div>`;
            return;
        }

        // ② Logged in but no subscription (RETAILER without a plan)
        if (!s.subscribed && s.role === 'ROLE_RETAILER') {
            body.innerHTML = `<div class="ft-gate">
                <div class="ft-gate-icon premium"><i class="fas fa-crown"></i></div>
                <h3>Premium Feature</h3>
                <p>The FlowTrack AI Assistant is available exclusively for subscribed retailers.
                   Upgrade your plan to unlock AI-powered inventory insights.</p>
                <a href="/business-profile/subscription" class="ft-gate-btn premium">
                    <i class="fas fa-arrow-up me-1"></i> Upgrade Now
                </a>
                <p class="ft-gate-hint">Already subscribed?
                   <a href="/retailer/dashboard">Refresh dashboard</a></p>
            </div>`;
            return;
        }

        // ③ Role not entitled (VENDOR, INVESTOR, etc.)
        if (!s.subscribed && s.role !== 'ROLE_PLATFORM_ADMIN') {
            body.innerHTML = `<div class="ft-gate">
                <div class="ft-gate-icon lock"><i class="fas fa-lock"></i></div>
                <h3>Not Available</h3>
                <p>The AI Assistant is currently available for FlowTrack retailers and administrators.</p>
            </div>`;
            return;
        }

        // ④ Fully unlocked – render chat UI
        this.renderChatUI(s);
    }

    // ── Chat UI ──────────────────────────────────────────────────────
    renderChatUI(s) {
        const body     = document.getElementById('ftBody');
        const subtitle = document.getElementById('ftSubtitle');

        // Update header subtitle with context
        const isAdmin = s.role === 'ROLE_PLATFORM_ADMIN';
        subtitle.textContent = isAdmin
            ? 'Platform Admin Assistant'
            : `${s.plan || ''} · ${s.businessName || 'Retailer'} Assistant`;

        const placeholder = isAdmin
            ? 'Ask about retailers, compliance, analytics…'
            : 'Ask about inventory, stock, vendors…';

        const welcome = isAdmin
            ? `Hello ${s.userName}! I'm your FlowTrack platform assistant. I can help you with retailer management, compliance checks, and platform analytics. What would you like to know?`
            : `Hello ${s.userName}! I'm your FlowTrack AI assistant for ${s.businessName || 'your business'}. I can help you with inventory insights, stock levels, vendor details, and business analytics. What would you like to know?`;

        body.innerHTML = `
            <div class="ft-messages" id="ftMessages">
                <div class="ft-typing" id="ftTyping">
                    <span></span><span></span><span></span>
                </div>
            </div>
            <div class="ft-input-row">
                <input class="ft-input" id="ftInput" type="text"
                       placeholder="${placeholder}" autocomplete="off"/>
                <button class="ft-send" id="ftSend">
                    <i class="fas fa-paper-plane"></i>
                </button>
            </div>`;

        // Wire up chat events
        document.getElementById('ftSend').addEventListener('click', () => this.send());
        document.getElementById('ftInput').addEventListener('keypress', e => {
            if (e.key === 'Enter') this.send();
        });

        // Add welcome message
        this.addMsg('bot', welcome);
    }

    // ── Messaging ────────────────────────────────────────────────────
    addMsg(sender, text) {
        const msgs = document.getElementById('ftMessages');
        if (!msgs) return;

        const div = document.createElement('div');
        div.className = `ft-msg ft-msg--${sender}`;
        div.innerHTML = `
            <div class="ft-msg-avatar">
                <i class="fas fa-${sender === 'bot' ? 'robot' : 'user'}"></i>
            </div>
            <div class="ft-msg-bubble">${this.escapeHtml(text)}</div>`;

        const typing = document.getElementById('ftTyping');
        msgs.insertBefore(div, typing);
        msgs.scrollTop = msgs.scrollHeight;
    }

    escapeHtml(text) {
        const d = document.createElement('div');
        d.textContent = text;
        return d.innerHTML;
    }

    setTyping(on) {
        const el = document.getElementById('ftTyping');
        if (el) el.classList.toggle('active', on);
        const msgs = document.getElementById('ftMessages');
        if (msgs) msgs.scrollTop = msgs.scrollHeight;
    }

    async send() {
        const input = document.getElementById('ftInput');
        const btn   = document.getElementById('ftSend');
        const msg   = input ? input.value.trim() : '';
        if (!msg) return;

        this.addMsg('user', msg);
        input.value = '';
        if (btn) btn.disabled = true;
        this.setTyping(true);

        try {
            const res  = await fetch('/api/chatbot/chat', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ message: msg })
            });

            const data = await res.json();
            this.setTyping(false);

            if (res.status === 403) {
                // Subscription lost mid-session – show gate
                this.renderBody();
                return;
            }

            this.addMsg('bot', data.response || 'Sorry, I could not process that request.');
        } catch (err) {
            console.error('Chatbot error:', err);
            this.setTyping(false);
            this.addMsg('bot', 'Sorry, I encountered a network error. Please try again.');
        } finally {
            if (btn) btn.disabled = false;
            const inp = document.getElementById('ftInput');
            if (inp) inp.focus();
        }
    }
}

// Init on DOM ready
document.addEventListener('DOMContentLoaded', () => { new ChatbotWidget(); });
