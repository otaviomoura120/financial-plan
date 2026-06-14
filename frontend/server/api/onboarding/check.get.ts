interface UserResponse {
  id: number
  name: string
  email: string
}

interface SpaceResponse {
  id: number
  name: string
  description?: string
}

export type OnboardingCheckResult =
  | { status: 'new_user' }
  | { status: 'no_spaces'; user: UserResponse }
  | { status: 'one_space'; user: UserResponse; spaces: SpaceResponse[] }
  | { status: 'multiple_spaces'; user: UserResponse; spaces: SpaceResponse[] }

export default defineEventHandler(async (event): Promise<OnboardingCheckResult> => {
    // debugger
  const { accessToken } = await useAuth0(event).getAccessToken()
  const config = useRuntimeConfig(event)
  const baseURL = config.public.apiBaseUrl
  const headers = { Authorization: `Bearer ${accessToken}` }

  let user: UserResponse | null = null

  try {
    user = await $fetch<UserResponse>('/users/me', { baseURL, headers })
  }
  catch (e: unknown) {
    const err = e as { response?: { status?: number } }

    if (err.response?.status !== 404)
      throw e
  }

  if (!user)
    return { status: 'new_user' }

  const spaces = await $fetch<SpaceResponse[]>(`/spaces/user/${user.id}`, { baseURL, headers })

  if (spaces.length === 0)
    return { status: 'no_spaces', user }

  if (spaces.length === 1)
    return { status: 'one_space', user, spaces }

  return { status: 'multiple_spaces', user, spaces }
})
