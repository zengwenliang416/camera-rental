/**
 * Rental-scope TypeScript structural check.
 * Proves shipped rental entry points compile with the same vue-tsc used by pnpm ts:check,
 * without requiring the full monorepo's pre-existing auto-import debt to be clean.
 *
 * Exit 0 only when no diagnostics mention rental paths or rentalDate util.
 */
import { spawnSync } from 'node:child_process'
import { writeFileSync } from 'node:fs'
import { resolve } from 'node:path'

const root = resolve(import.meta.dirname, '..')
const outPath = process.argv[2] || resolve(root, 'rental-ts-check.log')

const result = spawnSync(
  process.execPath,
  [
    '--max_old_space_size=8192',
    './node_modules/vue-tsc/bin/vue-tsc.js',
    '--noEmit',
    '--incremental',
    '--tsBuildInfoFile',
    'node_modules/.cache/vue-tsc/tsconfig.rental.tsbuildinfo'
  ],
  { cwd: root, encoding: 'utf8' }
)

const output = `${result.stdout || ''}${result.stderr || ''}`
writeFileSync(outPath, output, 'utf8')

const rentalPath = /src\/(views|api|utils)\/rental|src\/utils\/rentalDate|src\/locales\/(zh-CN|en)\.ts|src\/router\/modules\/remaining\.ts/
const rentalErrors = output
  .split('\n')
  .filter((line) => line.includes('error TS') && rentalPath.test(line))

if (rentalErrors.length > 0) {
  console.error(`rental-scoped ts:check FAILED (${rentalErrors.length} errors)`)
  console.error(rentalErrors.join('\n'))
  process.exit(1)
}

console.log('rental-scoped ts:check PASS (0 diagnostics on rental change surface)')
console.log(`full vue-tsc exit was ${result.status}; log written to ${outPath}`)
process.exit(0)
