/**
 * 日期工具：格式化 + 公历星座推算
 */

/** 格式化为 yyyy-MM-dd */
export function formatDate(d: Date | string | number = new Date()): string {
  const date = d instanceof Date ? d : new Date(d)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** 格式化为 yyyy-MM-dd HH:mm:ss */
export function formatDateTime(d: Date | string | number = new Date()): string {
  const date = d instanceof Date ? d : new Date(d)
  const base = formatDate(date)
  const hh = String(date.getHours()).padStart(2, '0')
  const mm = String(date.getMinutes()).padStart(2, '0')
  const ss = String(date.getSeconds()).padStart(2, '0')
  return `${base} ${hh}:${mm}:${ss}`
}

/**
 * 公历生日推算星座
 * @param dateStr yyyy-MM-dd
 */
export function getZodiac(dateStr: string): string {
  // 解析时避免时区漂移：拆字符串而不是 new Date(str)
  const [, m, d] = dateStr.split('-').map((n) => parseInt(n, 10))
  if (!m || !d) return '未知'
  const ranges: Array<[number, number, string]> = [
    [1, 19, '摩羯座'],
    [2, 18, '水瓶座'],
    [3, 20, '双鱼座'],
    [4, 19, '白羊座'],
    [5, 20, '金牛座'],
    [6, 21, '双子座'],
    [7, 22, '巨蟹座'],
    [8, 22, '狮子座'],
    [9, 22, '处女座'],
    [10, 23, '天秤座'],
    [11, 22, '天蝎座'],
    [12, 21, '射手座'],
    [12, 31, '摩羯座']
  ]
  for (const [mm, dd, name] of ranges) {
    if (m < mm || (m === mm && d <= dd)) return name
  }
  return '摩羯座'
}

/** 是否未成年（< 14 岁） */
export function isMinor(birthday: string, today: Date = new Date()): boolean {
  const [y, m, d] = birthday.split('-').map((n) => parseInt(n, 10))
  if (!y) return false
  let age = today.getFullYear() - y
  const monthDiff = today.getMonth() + 1 - m
  if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < d)) age--
  return age < 14
}
