export async function backendFetch<T = unknown>(
  request: Parameters<typeof $fetch>[0],
  options?: Parameters<typeof $fetch>[1],
): Promise<T> {
  try {
    // Backend URLs are external, so Nitro's internal route typing cannot infer the response
    return await $fetch<T>(request, options) as T
  }
  catch (e) {
    const fetchError = e as { statusCode?: number; data?: unknown }

    throw createError({
      statusCode: fetchError.statusCode ?? 500,
      statusMessage: typeof fetchError.data === 'string' ? fetchError.data : 'unknown',
      data: fetchError.data,
    })
  }
}
