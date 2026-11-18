package com.example.shine.ui.navigation

import androidx.navigation3.runtime.NavKey

/**
 * Xóa các phần tử trên đỉnh ngăn xếp cho đến khi chạm tới [key] được chỉ định.
 * Nếu [key] không tồn tại trong ngăn xếp, không có gì thay đổi.
 */
fun MutableList<NavKey>.popUntil(key: NavKey) {
    if (key !in this) return
    while (isNotEmpty() && last() != key) {
        removeAt(lastIndex)
    }
}

/**
 * Xóa sạch ngăn xếp và đặt [key] làm điểm đến duy nhất.
 */
fun MutableList<NavKey>.resetTo(key: NavKey) {
    clear()
    add(key)
}

/**
 * Pop phần tử trên cùng nếu kích thước ngăn xếp lớn hơn 1 (giữ lại root destination).
 */
fun MutableList<NavKey>.popBack() {
    if (size > 1) removeLastOrNull()
}
