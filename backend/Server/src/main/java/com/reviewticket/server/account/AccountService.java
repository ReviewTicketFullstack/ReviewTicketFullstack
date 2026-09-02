package com.reviewticket.server.account;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.reviewticket.server.auth.ConflictException;
import com.reviewticket.server.auth.UnauthorizedException;
import com.reviewticket.server.domain.Role;
import com.reviewticket.server.domain.User;
import com.reviewticket.server.repository.UserRepository;

@Service
public class AccountService {

    private final UserRepository users;

    public AccountService(UserRepository users) {
        this.users = users;
    }

    /**
     * 닉네임(사장이면 가게 이름) 변경.
     *
     * 필터가 넘겨준 User 는 이미 영속 상태가 끝난 객체일 수 있으므로
     * 여기서 id 로 다시 읽는다. 그래야 변경이 실제로 반영된다.
     */
    @Transactional
    public String changeDisplayName(long userId, String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        User user = load(userId);

        if (name.isEmpty()) {
            throw new IllegalArgumentException(
                    user.getRole() == Role.OWNER ? "가게 이름을 입력해 주세요" : "닉네임을 입력해 주세요");
        }
        if (name.length() > 32) {
            throw new IllegalArgumentException("이름은 32자 이하여야 합니다");
        }
        // 지금 쓰는 이름을 그대로 다시 넣은 경우. 중복이라고 막으면 이상하다.
        if (name.equals(user.getDisplayName())) {
            return name;
        }
        if (users.existsByDisplayName(name)) {
            throw new ConflictException(
                    user.getRole() == Role.OWNER ? "이미 쓰이고 있는 가게 이름입니다" : "이미 쓰이고 있는 닉네임입니다");
        }

        user.changeDisplayName(name);
        return name;
    }

    private User load(long userId) {
        return users.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("로그인이 필요합니다"));
    }
}
