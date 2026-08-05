package com.reviewticket.server.auth;

/**
 * 메일 링크가 여는 HTML 페이지들.
 *
 * 이 페이지들은 React 앱과 별개다 — 메일 클라이언트가 여는 독립 페이지라
 * 프레임워크 없이 순수 HTML + inline JS 로 자립해야 한다.
 *
 * 핵심 원칙: GET 으로 여는 순간에는 아무것도 바뀌지 않는다. 실제 동작
 * (가입 확정, 비밀번호 변경)은 사용자가 버튼을 눌러 POST 할 때만 일어난다.
 * 메일 클라이언트나 백신이 링크를 미리 열어도 계정이 만들어지거나 바뀌지 않는다.
 *
 * 토큰은 사용자가 URL 로 넘긴 값이므로 HTML 속성에 넣을 때 escape 한다.
 */
final class AuthPages {

    private AuthPages() {
    }

    private static String escapeAttr(String raw) {
        return raw.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("'", "&#39;");
    }

    private static final String STYLE = """
            <style>
              body { font-family: sans-serif; max-width: 26rem; margin: 3rem auto; padding: 0 1.5rem; line-height: 1.6; color: #1e293b; }
              h1 { font-size: 1.4rem; }
              button { width: 100%; padding: 0.8rem; font-size: 1rem; font-weight: 600; color: #fff; background: #2563eb; border: 0; border-radius: 0.5rem; cursor: pointer; margin-top: 1rem; }
              button:disabled { background: #94a3b8; cursor: not-allowed; }
              input { width: 100%; box-sizing: border-box; padding: 0.6rem; margin-top: 0.4rem; border: 1px solid #cbd5e1; border-radius: 0.5rem; font-size: 1rem; }
              label { display: block; margin-top: 1rem; font-size: 0.9rem; font-weight: 600; }
              .hint { font-size: 0.85rem; color: #64748b; }
              .field-error { margin-top: 0.4rem; font-size: 0.8rem; color: #b91c1c; }
              .msg { margin-top: 1rem; padding: 0.8rem; border-radius: 0.5rem; }
              .ok { background: #dcfce7; color: #166534; }
              .bad { background: #fee2e2; color: #991b1b; }
              .hidden { display: none; }
            </style>
            """;

    /**
     * 회원가입 완료 페이지. 버튼을 눌러야 POST /api/auth/verify 가 호출돼
     * 실제 회원이 만들어진다. 성공하면 대기 중이던 앱이 폴링으로 감지한다.
     */
    static String signupVerify(String token) {
        return """
                <!doctype html><html lang="ko"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>회원가입 완료</title>%s</head><body>
                <h1>회원가입을 완료합니다</h1>
                <p class="hint">아래 버튼을 누르면 이메일 인증이 확인되고 회원가입이 끝납니다.</p>
                <input type="hidden" id="token" value="%s">
                <button id="btn" onclick="finish()">회원가입 완료하기</button>
                <div id="msg"></div>
                <script>
                  async function finish() {
                    var btn = document.getElementById('btn');
                    var msg = document.getElementById('msg');
                    var token = document.getElementById('token').value;
                    btn.disabled = true; msg.className = ''; msg.textContent = '처리 중…';
                    try {
                      var res = await fetch('/api/auth/verify', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ token: token })
                      });
                      var data = await res.json().catch(function(){ return {}; });
                      if (res.ok) {
                        btn.classList.add('hidden');
                        msg.className = 'msg ok';
                        msg.textContent = '회원가입이 완료되었습니다. 이 창을 닫고 로그인해 주세요.';
                      } else {
                        msg.className = 'msg bad';
                        msg.textContent = data.message || '처리에 실패했습니다.';
                        btn.disabled = false;
                      }
                    } catch (e) {
                      msg.className = 'msg bad';
                      msg.textContent = '서버에 연결할 수 없습니다.';
                      btn.disabled = false;
                    }
                  }
                </script>
                </body></html>
                """.formatted(STYLE, escapeAttr(token));
    }

    /**
     * 비밀번호 재설정 페이지. 2단계다.
     *   1) [인증하기] → 토큰 유효성 확인(부작용 없음) → 통과하면 폼을 연다
     *   2) 새 비밀번호 + 확인 입력 → [비밀번호 변경] → POST 로 실제 변경
     */
    static String passwordReset(String token) {
        return """
                <!doctype html><html lang="ko"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>비밀번호 재설정</title>%s</head><body>
                <h1>비밀번호 재설정</h1>
                <input type="hidden" id="token" value="%s">

                <div id="step1">
                  <p class="hint">본인 확인을 위해 아래 버튼을 눌러 주세요.</p>
                  <button id="verifyBtn" onclick="verify()">인증하기</button>
                </div>

                <form id="step2" class="hidden" onsubmit="submitReset(event)">
                  <p class="hint">새 비밀번호를 입력해 주세요.<br>비밀번호는 대문자, 소문자, 숫자, 특수문자를 모두 포함해 6자~14자 사이로 만들어 주세요.</p>
                  <label>새 비밀번호
                    <input type="password" id="pw" autocomplete="new-password" oninput="onPasswordInput()" required>
                  </label>
                  <div id="pwError" class="field-error"></div>
                  <label>새 비밀번호 확인
                    <input type="password" id="pw2" autocomplete="new-password" oninput="onConfirmInput()" required>
                  </label>
                  <div id="pw2Error" class="field-error"></div>
                  <button type="submit" id="changeBtn">비밀번호 변경</button>
                </form>

                <div id="msg"></div>
                <script>
                  var token = document.getElementById('token').value;
                  function show(cls, text) {
                    var m = document.getElementById('msg');
                    m.className = 'msg ' + cls; m.textContent = text;
                  }
                  // 줄바꿈이 필요한 곳에만 쓴다. 코드에 직접 적은 문구만 넘길 것 —
                  // 서버가 보낸 값은 show() 로 넣어야 한다(그쪽은 textContent 라 안전하다).
                  function showHtml(cls, html) {
                    var m = document.getElementById('msg');
                    m.className = 'msg ' + cls; m.innerHTML = html;
                  }

                  // 서버가 errorCode 만 보내므로(message 는 빠진다) 문구는 여기서 채운다.
                  // 문구는 아래 passwordError() 가 돌려주는 것과 같게 맞춘다 — 서버와
                  // 판정 순서가 같아 같은 조건에서 두 문구가 함께 뜰 수 있는데,
                  // 그때 표현까지 다르면 서로 다른 지적을 받은 것처럼 보인다.
                  var ERROR_TEXT = {
                    PASSWORD_TOO_SHORT: '6~14자리로 입력해주세요.',
                    PASSWORD_TOO_LONG: '6~14자리로 입력해주세요.',
                    PASSWORD_INVALID_CHAR: '사용할 수 없는 문자가 있습니다.',
                    PASSWORD_MISSING_UPPER: '대문자를 포함해주세요.',
                    PASSWORD_MISSING_LOWER: '소문자를 포함해주세요.',
                    PASSWORD_MISSING_DIGIT: '숫자를 포함해주세요.',
                    PASSWORD_MISSING_SPECIAL: '특수문자를 포함해주세요.'
                  };

                  // 요청량 제한(429)에 걸린 동안 두 버튼을 잠그고 남은 시간을 센다.
                  // 이 페이지가 보내는 요청은 세 번뿐이라 스스로 걸릴 일은 거의 없지만,
                  // 같은 IP 의 다른 요청이 물통을 비우면 여기도 함께 막힌다.
                  var blockTimer = null;
                  function startBlock(seconds) {
                    var until = Date.now() + (seconds || 0) * 1000;
                    var verifyBtn = document.getElementById('verifyBtn');
                    var changeBtn = document.getElementById('changeBtn');
                    if (blockTimer) { clearInterval(blockTimer); }

                    function tick() {
                      var left = Math.ceil((until - Date.now()) / 1000);
                      if (left <= 0) {
                        clearInterval(blockTimer); blockTimer = null;
                        verifyBtn.disabled = false; changeBtn.disabled = false;
                        show('', '');
                        return;
                      }
                      var m = Math.floor(left / 60);
                      var s = left %% 60;
                      show('bad', '요청이 너무 많습니다. ' + m + ':' + (s < 10 ? '0' + s : s)
                        + ' 후 다시 시도해주세요.');
                    }

                    verifyBtn.disabled = true;
                    changeBtn.disabled = true;
                    tick();
                    blockTimer = setInterval(tick, 1000);
                  }

                  // 가입 화면(SignUpForm 의 getPasswordError)과 같은 순서·문구를 쓴다.
                  // 이 페이지는 React 앱과 분리돼 있어 그 함수를 가져다 쓸 수 없다.
                  function passwordError(pw) {
                    if (!pw) return '';
                    if (!/[A-Z]/.test(pw)) return '대문자를 포함해주세요.';
                    if (!/[a-z]/.test(pw)) return '소문자를 포함해주세요.';
                    if (!/[0-9]/.test(pw)) return '숫자를 포함해주세요.';
                    if (!/[!@#$%%^&*]/.test(pw)) return '특수문자를 포함해주세요.';
                    if (/[^A-Za-z0-9!@#$%%^&*]/.test(pw)) return '사용할 수 없는 문자가 있습니다.';
                    if (pw.length < 6 || pw.length > 14) return '6~14자리로 입력해주세요.';
                    return '';
                  }
                  function onPasswordInput() {
                    document.getElementById('pwError').textContent =
                      passwordError(document.getElementById('pw').value);
                    onConfirmInput();
                  }
                  function onConfirmInput() {
                    var pw = document.getElementById('pw').value;
                    var pw2 = document.getElementById('pw2').value;
                    document.getElementById('pw2Error').textContent =
                      (pw2 && pw !== pw2) ? '비밀번호가 서로 다릅니다.' : '';
                  }

                  async function verify() {
                    var btn = document.getElementById('verifyBtn');
                    btn.disabled = true; show('', '확인 중…');
                    try {
                      var res = await fetch('/api/auth/password-reset/check?token=' + encodeURIComponent(token));
                      var data = await res.json().catch(function(){ return {}; });
                      if (res.status === 429) {
                        startBlock(data.retryAfterSeconds);
                      } else if (res.ok && data.valid) {
                        document.getElementById('step1').classList.add('hidden');
                        document.getElementById('step2').classList.remove('hidden');
                        document.getElementById('msg').textContent = '';
                      } else {
                        show('bad', '재설정 링크가 만료되었거나 이미 사용되었습니다. 다시 요청해 주세요.');
                      }
                    } catch (e) {
                      show('bad', '서버에 연결할 수 없습니다.');
                      btn.disabled = false;
                    }
                  }
                  async function submitReset(event) {
                    event.preventDefault();
                    var btn = document.getElementById('changeBtn');
                    var pw = document.getElementById('pw').value;
                    var pw2 = document.getElementById('pw2').value;
                    if (pw !== pw2) { show('bad', '새 비밀번호가 서로 다릅니다.'); return; }
                    btn.disabled = true; show('', '변경 중…');
                    try {
                      var res = await fetch('/api/auth/password-reset', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ token: token, newPassword: pw, newPasswordConfirm: pw2 })
                      });
                      var data = await res.json().catch(function(){ return {}; });
                      if (res.ok) {
                        document.getElementById('step2').classList.add('hidden');
                        showHtml('ok', '비밀번호가 변경되었습니다.<br>이 창을 닫고 새 비밀번호로 로그인해 주세요.');
                      } else if (res.status === 429) {
                        startBlock(data.retryAfterSeconds);
                      } else {
                        show('bad', ERROR_TEXT[data.errorCode] || data.message || '변경에 실패했습니다.');
                        btn.disabled = false;
                      }
                    } catch (e) {
                      show('bad', '서버에 연결할 수 없습니다.');
                      btn.disabled = false;
                    }
                  }
                </script>
                </body></html>
                """.formatted(STYLE, escapeAttr(token));
    }
}
