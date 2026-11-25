package com.expressive.oneuicalc.model

enum class ButtonRole(val displayName: String, val subtitle: String) {
    NUMBER("Numbers (0–9, .)", "Primary digit keys and decimal separator"),
    OPERATOR("Operators (+ − × ÷)", "Arithmetic action keys"),
    UTILITY("Utility (C, (), %, ⌫)", "Clear, backspace, parentheses, percentage"),
    EQUALS("Equals (=)", "Primary hero calculation key"),
    SCIENTIFIC("Scientific (sin, √, π…)", "Advanced math & secondary keypad keys");

    companion object {
        fun fromEntryName(entryName: String?): ButtonRole {
            if (entryName == null) return NUMBER
            return when (entryName) {
                "calc_keypad_btn_equal" -> EQUALS
                "calc_keypad_btn_add",
                "calc_keypad_btn_sub",
                "calc_keypad_btn_mul",
                "calc_keypad_btn_div" -> OPERATOR
                "calc_keypad_btn_clear",
                "calc_keypad_btn_backspace",
                "calc_keypad_btn_parenthesis",
                "calc_keypad_btn_percentage",
                "converter_keypad_btn_clear",
                "converter_keypad_btn_backspace",
                "converter_keypad_btn_previous",
                "converter_keypad_btn_next",
                "converter_keypad_btn_plusminus" -> UTILITY
                "calc_keypad_btn_1st_2nd",
                "calc_keypad_btn_deg_rad",
                "calc_keypad_btn_root",
                "calc_keypad_btn_root_another_font_default",
                "calc_keypad_btn_abs",
                "calc_keypad_btn_sin",
                "calc_keypad_btn_cos",
                "calc_keypad_btn_tan",
                "calc_keypad_btn_pie",
                "calc_keypad_btn_ln",
                "calc_keypad_btn_log",
                "calc_keypad_btn_1_x",
                "calc_keypad_btn_e",
                "calc_keypad_btn_e_x",
                "calc_keypad_btn_x_2",
                "calc_keypad_btn_x_y",
                "calc_keypad_btn_plusminus" -> SCIENTIFIC
                else -> NUMBER
            }
        }
    }
}
