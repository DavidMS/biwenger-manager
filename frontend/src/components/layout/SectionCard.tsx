import { useId, type ReactNode } from 'react'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

type SectionCardProps = {
  title: string
  description?: string
  children: ReactNode
}

/**
 * A card that is a labelled region with a real heading, so screen readers can jump to it and
 * tests can find it by role: `getByRole('region', { name: 'Data sync' })`.
 */
export function SectionCard({
  title,
  description,
  children,
}: SectionCardProps) {
  const titleId = useId()
  return (
    <Card role="region" aria-labelledby={titleId}>
      <CardHeader>
        <CardTitle id={titleId} role="heading" aria-level={2}>
          {title}
        </CardTitle>
        {description && <CardDescription>{description}</CardDescription>}
      </CardHeader>
      <CardContent className="space-y-4">{children}</CardContent>
    </Card>
  )
}
