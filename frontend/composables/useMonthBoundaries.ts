function toLocalDateString(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

export function currentMonthValue() {
  const now = new Date()

  return toLocalDateString(new Date(now.getFullYear(), now.getMonth(), 1))
}

export function lastDayOfMonthOf(yearMonthFirstDay: string) {
  const year = Number(yearMonthFirstDay.slice(0, 4))
  const month = Number(yearMonthFirstDay.slice(5, 7))

  return toLocalDateString(new Date(year, month, 0))
}
