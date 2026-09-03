package com.reviewticket.server.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.auth.ConflictException;
import com.reviewticket.server.auth.ForbiddenException;
import com.reviewticket.server.auth.UnauthorizedException;
import com.reviewticket.server.auth.ValidationException;
import com.reviewticket.server.domain.Role;
import com.reviewticket.server.domain.User;
import com.reviewticket.server.repository.PendingSignupRepository;
import com.reviewticket.server.repository.UserRepository;

@Service
public class AccountService {

    private final UserRepository users;
    private final PendingSignupRepository pendings;

    public AccountService(UserRepository users, PendingSignupRepository pendings) {
        this.users = users;
        this.pendings = pendings;
    }

    /**
     * 닉네임 변경. 고객 전용이다.
     *
     * 필터가 넘겨준 User 는 이미 영속 상태가 끝난 객체일 수 있으므로
     * 여기서 id 로 다시 읽는다. 그래야 변경이 실제로 반영된다.
     *
     * 사장은 이 API 를 쓸 수 없다 — 가게 이름은 store 표의 store_name 이 정답이고,
     * 여기서 바꾸면 users.display_name 만 바뀌어 store_name 과 어긋난다. 화면에
     * 사장용 버튼을 안 두는 것만으로는 막히지 않는다(요청은 화면을 거치지 않고도
     * 보낼 수 있다). 사장의 이름 변경은 가게 관리 API(PATCH /api/stores/me) 로만
     * 되게 여기서 직접 막는다.
     */
    @Transactional
    public String changeDisplayName(long userId, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        User user = load(userId);

        if (user.getRole() == Role.OWNER) {
            throw new ForbiddenException("NOT_CUSTOMER",
                    "사장 계정은 이 API로 이름을 바꿀 수 없습니다. 가게 이름은 PATCH /api/stores/me 로 바꿔야 합니다");
        }

        if (name.isEmpty()) {
            throw new ValidationException("NAME_REQUIRED", "닉네임을 입력해 주세요");
        }
        if (name.length() > 32) {
            throw new ValidationException("NAME_TOO_LONG", "이름은 32자 이하여야 합니다");
        }
        // 지금 쓰는 이름을 그대로 다시 넣은 경우. 중복이라고 막으면 이상하다.
        if (name.equals(user.getDisplayName())) {
            return name;
        }
        // 가입 대기 중인 이름도 예약된 것으로 본다. 가입 화면의 중복 검사
        // (AuthService.displayNameTaken)와 같은 기준이어야 한다. users 만 보면
        // 대기자의 이름을 가로챌 수 있고, 그러면 그 사람이 인증 링크를 눌렀을 때
        // verify() 에서 충돌이 나 가입 자체가 무산된다.
        if (users.existsByDisplayName(name) || pendings.existsByDisplayName(name)) {
            throw new ConflictException("NAME_TAKEN", "이미 쓰이고 있는 닉네임입니다");
        }

        user.changeDisplayName(name);
        return name;
    }

    private User load(long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("UNAUTHORIZED", "로그인이 필요합니다"));
    }
}
