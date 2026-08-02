import type { User } from "@/entities/user";

interface LoginRequest {
  email: string;
  password: string;
}

export async function login(data: LoginRequest): Promise<User> {
  const response = await fetch(
    "/api/auth/login", // api example. dto check.
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify(data),
    },
  );

  if (!response.ok) {
    throw new Error("로그인 실패");
  }

  return response.json();
}
