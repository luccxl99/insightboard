const number = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 2 })
const compact = new Intl.NumberFormat('pt-BR', { notation: 'compact', maximumFractionDigits: 1 })
const dateTime = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

export const fmtNumber = (n: number) => number.format(n)
export const fmtCompact = (n: number) => compact.format(n)
export const fmtDateTime = (iso: string) => dateTime.format(new Date(iso))
