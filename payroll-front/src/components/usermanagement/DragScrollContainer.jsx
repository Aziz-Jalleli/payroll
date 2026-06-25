import { useRef } from "react";
import "../../styles/usermanagement/DragScrollContainer.css";

/**
 * Wraps wide content (like a table) in a horizontally scrollable box.
 * Supports the native scrollbar/trackpad/shift+wheel as usual, AND
 * click-and-drag scrolling with the mouse - press anywhere on the
 * background (not on an interactive element) and drag left/right.
 *
 * Dragging is only engaged from the table's background area, not from
 * inputs, selects, or buttons inside it - otherwise clicking a dropdown
 * or typing in a filter would also start a drag and fight with normal
 * interaction.
 */
function DragScrollContainer({ children, className = "" }) {
  const scrollRef = useRef(null);
  const dragState = useRef({ isDragging: false, startX: 0, startScrollLeft: 0 });

  function isInteractiveTarget(target) {
    return Boolean(target.closest("button, select, input, a, [role='button']"));
  }

  function handleMouseDown(event) {
    if (isInteractiveTarget(event.target)) return;
    const container = scrollRef.current;
    if (!container) return;

    dragState.current = {
      isDragging: true,
      startX: event.pageX,
      startScrollLeft: container.scrollLeft,
    };
    container.classList.add("drag-scroll-container--dragging");
  }

  function handleMouseMove(event) {
    if (!dragState.current.isDragging) return;
    const container = scrollRef.current;
    if (!container) return;

    event.preventDefault();
    const delta = event.pageX - dragState.current.startX;
    container.scrollLeft = dragState.current.startScrollLeft - delta;
  }

  function endDrag() {
    if (!dragState.current.isDragging) return;
    dragState.current.isDragging = false;
    scrollRef.current?.classList.remove("drag-scroll-container--dragging");
  }

  return (
    <div
      ref={scrollRef}
      className={`drag-scroll-container ${className}`.trim()}
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={endDrag}
      onMouseLeave={endDrag}
    >
      {children}
    </div>
  );
}

export default DragScrollContainer;