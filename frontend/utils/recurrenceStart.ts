// Mirrors the backend RecurrenceStartPolicy: a recurring bill or card subscription cannot start more than
// five years ago, because every month since the start date is generated at once.
export const RECURRENCE_MAX_YEARS_IN_PAST = 5

// Same arithmetic as Java's LocalDate.minusYears: Feb 29 falls back to Feb 28 instead of rolling into March
export function recurrenceMinStartDate(): string {
  const today = new Date()
  const year = today.getFullYear() - RECURRENCE_MAX_YEARS_IN_PAST
  const month = today.getMonth()
  const lastDayOfMonth = new Date(year, month + 1, 0).getDate()
  const day = Math.min(today.getDate(), lastDayOfMonth)

  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

// The backend only checks a start date when it changes, so a recurrence that aged past the limit keeps
// its original date and stays editable
export function recurrenceStartRule(originalStartDate?: string | null) {
  return (v: string) => {
    if (!v || v === originalStartDate) {
      return true
    }

    const minDate = recurrenceMinStartDate()

    if (v >= minDate) {
      return true
    }

    const [year, month, day] = minDate.split('-')

    return `A data de início não pode ser anterior a ${day}/${month}/${year} (${RECURRENCE_MAX_YEARS_IN_PAST} anos atrás)`
  }
}
