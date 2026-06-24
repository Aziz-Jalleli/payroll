/**
 * AvoCarbonLogo
 * Recreates the AVO Carbon GROUP brand mark as inline SVG.
 *
 * Props:
 *   width  – rendered width  (default: 200)
 *   height – rendered height (default: 80)
 */
export default function AvoCarbonLogo({ width = 200, height = 80 }) {
  return (
    <svg
      width={width}
      height={height}
      viewBox="0 0 200 80"
      xmlns="http://www.w3.org/2000/svg"
      role="img"
      aria-label="AVO Carbon Group"
    >
      <title>AVO Carbon Group</title>

      {/* ── AVO (blue, italic, bold) ── */}
      <text
        x="4"
        y="44"
        fontFamily="'Arial Black', 'Helvetica Neue', Arial, sans-serif"
        fontWeight="900"
        fontStyle="italic"
        fontSize="48"
        fill="#2E86C1"
        letterSpacing="-1"
      >
        AVO
      </text>

      {/* ── Carbon GROUP on second line ── */}
      <text
        x="4"
        y="72"
        fontFamily="'Arial Black', 'Helvetica Neue', Arial, sans-serif"
        fontWeight="900"
        fontStyle="italic"
        fontSize="28"
        letterSpacing="0"
      >
        <tspan fill="#3D3D3D">Carbon </tspan>
        <tspan fill="#E8720C" fontSize="18" dy="2">GROUP</tspan>
      </text>
    </svg>
  );
}