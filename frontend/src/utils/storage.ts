/**
 * uni.storage 封装：支持 TTL（毫秒），支持类型推断
 * 注意：uni.getStorageSync 返回值在不同平台行为略有差异，此处统一以 string 序列化兜底
 */

interface StorageEntry<T> {
  v: T
  e?: number  // 过期时间戳（毫秒），不传表示永久
}

/**
 * 写入
 * @param key  键名
 * @param value 值（任意可 JSON 序列化）
 * @param ttl  过期毫秒数；不传表示永久
 */
export function setStorage<T>(key: string, value: T, ttl?: number): void {
  const entry: StorageEntry<T> = { v: value }
  if (ttl && ttl > 0) {
    entry.e = Date.now() + ttl
  }
  try {
    uni.setStorageSync(key, JSON.stringify(entry))
  } catch (e) {
    console.warn('[storage] set 失败', key, e)
  }
}

/**
 * 读取；过期或不存在返回 null
 */
export function getStorage<T>(key: string): T | null {
  try {
    const raw = uni.getStorageSync(key)
    if (!raw) return null
    const entry = JSON.parse(raw as string) as StorageEntry<T>
    if (entry.e && Date.now() > entry.e) {
      uni.removeStorageSync(key)
      return null
    }
    return entry.v
  } catch (e) {
    console.warn('[storage] get 失败', key, e)
    return null
  }
}

/** 删除单个 */
export function removeStorage(key: string): void {
  try {
    uni.removeStorageSync(key)
  } catch (e) {
    console.warn('[storage] remove 失败', key, e)
  }
}

/** 清空全部（慎用） */
export function clearStorage(): void {
  try {
    uni.clearStorageSync()
  } catch (e) {
    console.warn('[storage] clear 失败', e)
  }
}
