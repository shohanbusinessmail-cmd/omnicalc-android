# Formula Catalog

Model: `feature/formulas/FormulaCatalog.kt` — bilingual metadata + an engine
expression per formula. Results are **never stored**; each computation runs
`Engine.evaluate(expr, EvalContext(variables = user inputs))` and is then
formatted through `DisplayFormatter`.

## Entries (18)

### Geometry
| id | name (en / bn) | expression | variables |
|---|---|---|---|
| rectangle-area | Rectangle area / আয়তক্ষেত্রের ক্ষেত্রফল | `l * w` | l, w |
| rectangle-perimeter | Rectangle perimeter / পরিসীমা | `2 * (l + w)` | l, w |
| triangle-area | Triangle area / ত্রিভুজের ক্ষেত্রফল | `1/2 * b * h` | b, h |
| circle-area | Circle area / বৃত্তের ক্ষেত্রফল | `π * r^2` | r |
| circle-circumference | Circle circumference / পরিসীমা | `2 * π * r` | r |
| rectangle-diagonal | Rectangle diagonal / কর্ণ | `sqrt(l^2 + w^2)` | l, w |
| trapezoid-area | Trapezoid area / সমদ্বিবাহু চতুর্ভুজ | `(a + b)/2 * h` | a, b, h |
| sphere-volume | Sphere volume / গোলকের আয়তন | `4/3 * π * r^3` | r |
| cylinder-volume | Cylinder volume / বেলনের আয়তন | `π * r^2 * h` | r, h |

### Algebra
| id | name | expression | variables |
|---|---|---|---|
| quadratic-root-plus | Quadratic formula (root 1) | `(-b + sqrt(b^2 - 4*a*c)) / (2*a)` | a, b, c |
| quadratic-root-minus | Quadratic formula (root 2) | `(-b - sqrt(b^2 - 4*a*c)) / (2*a)` | a, b, c |
| slope | Line slope / রেখার ঢাল | `(y2 - y1) / (x2 - x1)` | x1, y1, x2, y2 |
| percent-of | Percent of a number / শতকরা অংশ | `p/100 * n` | p, n |

### Finance
| id | name | expression | variables |
|---|---|---|---|
| simple-interest | Simple interest / সাধারণ সুদ | `p * r * t / 100` | p, r, t |
| compound-amount | Compound amount / চক্রবৃদ্ধি পরিমাণ | `p * (1 + r/100)^t` | p, r, t |
| vat-amount | VAT / tax amount | `p * v / 100` | p, **v = user-entered rate** |
| percentage-change | Percentage change / শতকরা পরিবর্তন | `(new - old) / old * 100` | old, new |

### Physics
| id | name | expression | variables |
|---|---|---|---|
| speed | Speed / গতি | `d / t` | d, t |

## Rules

- Variable identifiers never collide with functions/constants (`e` is not a
  variable name anywhere; `exp/ln/log/mod/root/sum/…` avoided).
- No formula embeds a result or a policy rate: VAT/interest **rates are
  inputs** (regional rules live in Tools, docs/REGIONAL_RULES.md).
- Failed evaluations surface the localized engine error (e.g. domain error
  when the quadratic discriminant is negative) — nothing is faked.
- Favorites store `type="formula", payload=<formula id>`; Library taps jump
  straight to that formula’s expanded detail.
- Committed results are recorded to history with `toolType="formula"` and the
  variable binding in `contextPairs` for reproducibility.
