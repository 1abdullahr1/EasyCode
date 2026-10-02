#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

const COURSES_DATA: &str = include_str!("../ui/assets/courses.json");

#[tauri::command]
fn get_courses() -> String {
    COURSES_DATA.to_string()
}

#[tauri::command]
fn open_in_browser(url: String) {
    let _ = open::that(&url);
}

fn main() {
    std::panic::set_hook(Box::new(|panic_info| {
        let temp_dir = std::env::temp_dir();
        let log_file = temp_dir.join("vu_course_hub.log");
        let _ = std::fs::write(log_file, format!("Panic: {}\n", panic_info));
    }));

    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![get_courses, open_in_browser])
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
