// VU Course Hub Desktop Application Logic

// Supported Playback Speeds from 1.0x to 4.0x
const PLAYBACK_SPEEDS = [1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 3.5, 4.0];

// Application State
const state = {
  courses: [],
  departments: ["All"],
  selectedDept: "All",
  searchQuery: "",
  selectedCourse: null,
  currentLectureIndex: 1,
  savedCodes: new Set(),
  enrolledCodes: new Set(),
  watchHistory: [],
  lastWatched: null,
  savedSubtab: "all",
  resourceSubtab: "handouts",
  resourceQuery: "",
  playbackSpeed: 1.0,
  isSidebarCollapsed: false,
  isPlaylistCollapsed: false,
  theme: "dark",
  currentTab: "home"
};

// Official Resources List
const officialLinks = [
  {
    title: "Virtual University LMS (VULMS)",
    url: "https://vulms.vu.edu.pk",
    description: "Access course announcements, assignments, quizzes, and gradebooks."
  },
  {
    title: "VU Official Portal",
    url: "https://www.vu.edu.pk",
    description: "University announcements, academic calendar, and official programs."
  },
  {
    title: "VU Digital Library",
    url: "https://library.vu.edu.pk",
    description: "Research papers, e-books, journals, and comprehensive digital archives."
  },
  {
    title: "VU Date Sheet System",
    url: "https://datesheet.vu.edu.pk",
    description: "Midterm and final examination date sheet scheduling portal."
  },
  {
    title: "Official YouTube Channel",
    url: "https://www.youtube.com/@VirtualUniversityofPakistan",
    description: "Official channel broadcasts, live sessions, and ceremony coverage."
  },
  {
    title: "Student Notice Board",
    url: "https://www.vu.edu.pk/NewsNotice.aspx",
    description: "Urgent announcements, deadline extensions, and university notifications."
  },
  {
    title: "VU Admissions Portal",
    url: "https://www.vu.edu.pk/apply",
    description: "Program criteria, fee schedules, and new student admission portal."
  }
];

// Academic Tools List
const academicTools = [
  {
    title: "VU GPA & CGPA Calculator",
    url: "https://www.google.com/search?q=VU+GPA+CGPA+calculator",
    description: "Calculate your semester GPA and cumulative CGPA based on official grading scales."
  },
  {
    title: "VU Past Papers & Solved Solutions",
    url: "https://www.google.com/search?q=Virtual+University+past+papers+filetype:pdf",
    description: "Access past midterm and final examination papers, question banks, and review guides."
  },
  {
    title: "Academic Calendar & Schedule",
    url: "https://www.vu.edu.pk/pages/academiccalendar.aspx",
    description: "Official semester dates, course selection deadlines, and examination schedules."
  },
  {
    title: "VU Student Support Services",
    url: "https://support.vu.edu.pk",
    description: "Submit tickets for technical support, fee queries, and degree processing."
  }
];

// Helper to get Tauri invoke function across different injection models
function getTauriInvoke() {
  if (window.__TAURI__) {
    if (typeof window.__TAURI__.invoke === "function") return window.__TAURI__.invoke;
    if (window.__TAURI__.tauri && typeof window.__TAURI__.tauri.invoke === "function") return window.__TAURI__.tauri.invoke;
  }
  return null;
}

// Open URL externally via Tauri shell / Rust command / browser
function openExternal(url) {
  const invoke = getTauriInvoke();
  if (invoke) {
    invoke("open_in_browser", { url }).catch(() => {
      window.open(url, "_blank");
    });
  } else {
    window.open(url, "_blank");
  }
}

// Format Course Code
function getCourseCode(course) {
  if (course.courseCode && course.courseCode.trim()) return course.courseCode.trim();
  const match = (course.title || "").match(/^([A-Za-z]{2,4}\d{3})/i);
  return match ? match[1].toUpperCase() : "VU";
}

// Clean Course Title
function getCleanTitle(course) {
  const code = getCourseCode(course);
  let title = (course.title || "").trim();
  if (title.toUpperCase().startsWith(code.toUpperCase())) {
    title = title.substring(code.length).trim();
    title = title.replace(/^[-:–—\s]+/, "");
  }
  return title || course.title || "Course Details";
}

// Format Department
function getDepartment(course) {
  return course.department && course.department.trim() ? course.department.trim() : "General";
}

// Course Thumbnail Helper with reliable fallback
function getCourseThumbnail(course) {
  if (course.thumbnailUrl && course.thumbnailUrl.startsWith("http")) {
    return course.thumbnailUrl;
  }
  if (course.firstVideoId) {
    return `https://i.ytimg.com/vi/${course.firstVideoId}/hqdefault.jpg`;
  }
  return "icons/icon.png";
}

// Generate Embed URL for YouTube
function getEmbedUrl(playlistId, lectureIndex, firstVideoId) {
  const cleanPid = (playlistId || "").trim();
  if (lectureIndex <= 1 && firstVideoId && firstVideoId.trim()) {
    const vid = firstVideoId.trim();
    if (cleanPid) {
      return `https://www.youtube-nocookie.com/embed/${vid}?autoplay=1&enablejsapi=1&rel=0&list=${cleanPid}`;
    }
    return `https://www.youtube-nocookie.com/embed/${vid}?autoplay=1&enablejsapi=1&rel=0`;
  }
  const zeroIndex = Math.max(0, lectureIndex - 1);
  return `https://www.youtube-nocookie.com/embed/videoseries?list=${cleanPid}&index=${zeroIndex}&autoplay=1&enablejsapi=1&rel=0`;
}

// Tab Switching
function switchTab(tabId) {
  state.currentTab = tabId;

  document.querySelectorAll(".nav-item").forEach(item => {
    if (item.dataset.tab === tabId) {
      item.classList.add("active");
    } else {
      item.classList.remove("active");
    }
  });

  document.querySelectorAll(".view-panel").forEach(panel => {
    panel.classList.remove("active");
  });

  const targetPanel = document.getElementById(`view-${tabId}`);
  if (targetPanel) {
    targetPanel.classList.add("active");
  }

  if (tabId === "home") {
    renderHomeView();
  } else if (tabId === "saved") {
    renderSavedView();
  } else if (tabId === "resources") {
    renderResourcesView();
  }
}

// Sidebar Collapse / Expand Toggle
function toggleSidebar() {
  const sidebar = document.getElementById("app-sidebar");
  state.isSidebarCollapsed = !state.isSidebarCollapsed;
  if (state.isSidebarCollapsed) {
    sidebar.classList.add("collapsed");
    document.getElementById("btn-sidebar-toggle").title = "Expand Sidebar";
  } else {
    sidebar.classList.remove("collapsed");
    document.getElementById("btn-sidebar-toggle").title = "Collapse Sidebar";
  }
  try {
    localStorage.setItem("vu_sidebar_collapsed", state.isSidebarCollapsed ? "true" : "false");
  } catch (e) {}
}

function initSidebarState() {
  try {
    state.isSidebarCollapsed = localStorage.getItem("vu_sidebar_collapsed") === "true";
  } catch (e) {
    state.isSidebarCollapsed = false;
  }
  const sidebar = document.getElementById("app-sidebar");
  if (state.isSidebarCollapsed) {
    sidebar.classList.add("collapsed");
    document.getElementById("btn-sidebar-toggle").title = "Expand Sidebar";
  }
}

// Collapsible Video Playlist Toggle
function togglePlaylist(forceState) {
  const playerLayout = document.getElementById("player-active-state");
  const label = document.getElementById("playlist-toggle-label");

  if (typeof forceState === "boolean") {
    state.isPlaylistCollapsed = forceState;
  } else {
    state.isPlaylistCollapsed = !state.isPlaylistCollapsed;
  }

  if (state.isPlaylistCollapsed) {
    playerLayout.classList.add("playlist-collapsed");
    if (label) label.textContent = "Show Playlist";
  } else {
    playerLayout.classList.remove("playlist-collapsed");
    if (label) label.textContent = "Hide Playlist";
  }

  try {
    localStorage.setItem("vu_playlist_collapsed", state.isPlaylistCollapsed ? "true" : "false");
  } catch (e) {}
}

function initPlaylistState() {
  try {
    state.isPlaylistCollapsed = localStorage.getItem("vu_playlist_collapsed") === "true";
  } catch (e) {
    state.isPlaylistCollapsed = false;
  }
  togglePlaylist(state.isPlaylistCollapsed);
}

// Explicit Fullscreen Control
function toggleFullscreen() {
  const container = document.getElementById("video-container-box");
  if (!container) return;

  if (!document.fullscreenElement && !document.webkitFullscreenElement && !document.msFullscreenElement) {
    if (container.requestFullscreen) {
      container.requestFullscreen();
    } else if (container.webkitRequestFullscreen) {
      container.webkitRequestFullscreen();
    } else if (container.msRequestFullscreen) {
      container.msRequestFullscreen();
    }
  } else {
    if (document.exitFullscreen) {
      document.exitFullscreen();
    } else if (document.webkitExitFullscreen) {
      document.webkitExitFullscreen();
    } else if (document.msExitFullscreen) {
      document.msExitFullscreen();
    }
  }
}

// Handle Fullscreen Change Events
function handleFullscreenChange() {
  const isFs = !!(document.fullscreenElement || document.webkitFullscreenElement || document.msFullscreenElement);
  const btn = document.getElementById("btn-fullscreen");
  if (btn) {
    btn.textContent = isFs ? "Exit Fullscreen" : "Fullscreen";
  }
}

// Playback Speed Controls (1x to 4x)
function applyPlaybackRate(speed) {
  state.playbackSpeed = speed;
  const iframe = document.getElementById("player-iframe");
  if (iframe && iframe.contentWindow) {
    iframe.contentWindow.postMessage(JSON.stringify({
      event: "command",
      func: "setPlaybackRate",
      args: [speed]
    }), "*");
  }

  const speedDisplay = document.getElementById("speed-display");
  if (speedDisplay) {
    const formatted = speed % 1 === 0 ? `${speed.toFixed(1)}x` : `${speed}x`;
    speedDisplay.textContent = formatted;
  }

  try {
    localStorage.setItem("vu_playback_speed", String(speed));
  } catch (e) {}
}

function increaseSpeed() {
  const current = state.playbackSpeed;
  const next = PLAYBACK_SPEEDS.find(s => s > current);
  if (next !== undefined) {
    applyPlaybackRate(next);
  }
}

function decreaseSpeed() {
  const current = state.playbackSpeed;
  const reversed = [...PLAYBACK_SPEEDS].reverse();
  const prev = reversed.find(s => s < current);
  if (prev !== undefined) {
    applyPlaybackRate(prev);
  }
}

function cycleSpeed() {
  const cycleList = [1.0, 1.25, 1.5, 2.0, 2.5, 3.0, 4.0];
  const idx = cycleList.indexOf(state.playbackSpeed);
  const nextIdx = (idx + 1) % cycleList.length;
  applyPlaybackRate(cycleList[nextIdx]);
}

// Data Persistence (Bookmarks, Enrolled Courses, Watch History)
function loadUserData() {
  try {
    const saved = localStorage.getItem("vu_saved_codes");
    if (saved) state.savedCodes = new Set(JSON.parse(saved));
  } catch (e) { state.savedCodes = new Set(); }

  try {
    const enrolled = localStorage.getItem("vu_enrolled_codes");
    if (enrolled) state.enrolledCodes = new Set(JSON.parse(enrolled));
  } catch (e) { state.enrolledCodes = new Set(); }

  try {
    const history = localStorage.getItem("vu_watch_history");
    if (history) state.watchHistory = JSON.parse(history);
  } catch (e) { state.watchHistory = []; }

  try {
    const last = localStorage.getItem("vu_last_watched");
    if (last) state.lastWatched = JSON.parse(last);
  } catch (e) { state.lastWatched = null; }

  try {
    const sp = parseFloat(localStorage.getItem("vu_playback_speed") || "1.0");
    if (sp >= 1.0 && sp <= 4.0) state.playbackSpeed = sp;
  } catch (e) {}

  updateSavedBadge();
}

function saveUserData() {
  try {
    localStorage.setItem("vu_saved_codes", JSON.stringify(Array.from(state.savedCodes)));
  } catch (e) {}
  try {
    localStorage.setItem("vu_enrolled_codes", JSON.stringify(Array.from(state.enrolledCodes)));
  } catch (e) {}
  try {
    localStorage.setItem("vu_watch_history", JSON.stringify(state.watchHistory));
  } catch (e) {}
  try {
    if (state.lastWatched) {
      localStorage.setItem("vu_last_watched", JSON.stringify(state.lastWatched));
    }
  } catch (e) {}
  updateSavedBadge();
}

function updateSavedBadge() {
  const badge = document.getElementById("saved-badge");
  const count = state.savedCodes.size;
  if (count > 0) {
    badge.textContent = count;
    badge.classList.remove("hidden");
  } else {
    badge.classList.add("hidden");
  }
}

function toggleBookmark(code) {
  if (state.savedCodes.has(code)) {
    state.savedCodes.delete(code);
  } else {
    state.savedCodes.add(code);
  }
  saveUserData();

  // Update card buttons across UI
  document.querySelectorAll(`[data-bookmark-code="${code}"]`).forEach(btn => {
    const isSaved = state.savedCodes.has(code);
    btn.textContent = isSaved ? "Saved" : "Save";
    if (isSaved) {
      btn.classList.add("saved");
    } else {
      btn.classList.remove("saved");
    }
  });

  if (state.currentTab === "saved") {
    renderSavedView();
  } else if (state.currentTab === "home") {
    renderHomeView();
  }
}

// Format relative time for watch history
function formatRelativeTime(timestamp) {
  if (!timestamp) return "Recently";
  const diff = Date.now() - timestamp;
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return "Just now";
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days === 1) return "Yesterday";
  if (days < 30) return `${days}d ago`;
  return new Date(timestamp).toLocaleDateString();
}

// Clear Watch History
function clearWatchHistory() {
  if (confirm("Are you sure you want to clear your entire watch history?")) {
    state.watchHistory = [];
    saveUserData();
    renderSavedView();
  }
}

// Theme Storage
function initTheme() {
  const saved = localStorage.getItem("vu_theme") || "dark";
  setTheme(saved);
}

function setTheme(theme) {
  state.theme = theme;
  document.body.className = theme === "light" ? "theme-light" : "theme-dark";
  document.getElementById("theme-status").textContent = theme === "light" ? "Light" : "Dark";
  try {
    localStorage.setItem("vu_theme", theme);
  } catch (e) {}
}

function toggleTheme() {
  setTheme(state.theme === "light" ? "dark" : "light");
}

// Play Course / Lecture
function playCourse(course, lectureIndex = 1) {
  state.selectedCourse = course;
  state.currentLectureIndex = lectureIndex;

  const code = getCourseCode(course);
  const title = getCleanTitle(course);
  const totalLectures = Math.max(1, Math.min(course.videoCount || 45, 100));
  const lecTitle = `Lecture ${String(lectureIndex).padStart(2, "0")} - ${title}`;
  const now = Date.now();

  // Add to enrolled courses
  state.enrolledCodes.add(code);

  // Update Last Watched
  state.lastWatched = {
    code: code,
    playlistId: course.playlistId,
    lectureIndex: lectureIndex,
    lectureTitle: lecTitle,
    timestamp: now
  };

  // Add to Watch History (keep max 50 recent items)
  state.watchHistory = state.watchHistory.filter(item => !(item.courseCode === code && item.lectureIndex === lectureIndex));
  state.watchHistory.unshift({
    playlistId: course.playlistId,
    courseCode: code,
    courseTitle: title,
    lectureIndex: lectureIndex,
    lectureTitle: lecTitle,
    timestamp: now,
    thumbnailUrl: getCourseThumbnail(course)
  });
  if (state.watchHistory.length > 50) {
    state.watchHistory = state.watchHistory.slice(0, 50);
  }

  saveUserData();
  switchTab("player");
  renderPlayer();
}

// Render Player View
function renderPlayer() {
  const course = state.selectedCourse;
  const activeState = document.getElementById("player-active-state");
  const emptyState = document.getElementById("player-empty-state");
  const indicator = document.getElementById("player-indicator");

  if (!course) {
    activeState.classList.add("hidden");
    emptyState.classList.remove("hidden");
    indicator.classList.add("hidden");
    return;
  }

  activeState.classList.remove("hidden");
  emptyState.classList.add("hidden");
  indicator.classList.remove("hidden");

  const code = getCourseCode(course);
  const title = getCleanTitle(course);
  const dept = getDepartment(course);
  const totalLectures = Math.max(1, Math.min(course.videoCount || 45, 100));

  document.getElementById("player-course-code").textContent = code;
  document.getElementById("player-course-dept").textContent = dept;
  document.getElementById("player-course-title").textContent = title;
  document.getElementById("player-active-lec-title").textContent = `${code} - Lecture ${String(state.currentLectureIndex).padStart(2, "0")}`;
  document.getElementById("playlist-count").textContent = `${totalLectures} lectures`;

  // Hide placeholder and load iframe
  const iframe = document.getElementById("player-iframe");
  const placeholder = document.getElementById("player-placeholder");
  const embedUrl = getEmbedUrl(course.playlistId, state.currentLectureIndex, course.firstVideoId);

  if (iframe.src !== embedUrl) {
    iframe.src = embedUrl;
  }
  placeholder.classList.add("hidden");

  // Re-apply current speed after iframe initializes
  setTimeout(() => {
    applyPlaybackRate(state.playbackSpeed);
  }, 750);

  // Render Playlist Items
  const listEl = document.getElementById("playlist-items-list");
  listEl.innerHTML = "";

  for (let num = 1; num <= totalLectures; num++) {
    const isCurrent = num === state.currentLectureIndex;
    const item = document.createElement("div");
    item.className = `playlist-item ${isCurrent ? "active" : ""}`;
    item.innerHTML = `
      <div class="playlist-item-left">
        <span class="lec-num">${String(num).padStart(2, "0")}</span>
        <span class="lec-name">${code} - Lecture ${String(num).padStart(2, "0")}</span>
      </div>
      <span class="lec-badge">${isCurrent ? "Playing" : "Play"}</span>
    `;
    item.onclick = () => {
      playCourse(course, num);
    };
    listEl.appendChild(item);

    if (isCurrent) {
      setTimeout(() => {
        item.scrollIntoView({ behavior: "smooth", block: "nearest" });
      }, 50);
    }
  }
}

// Next / Prev Lecture
function nextLecture() {
  if (!state.selectedCourse) return;
  const max = Math.max(1, Math.min(state.selectedCourse.videoCount || 45, 100));
  if (state.currentLectureIndex < max) {
    playCourse(state.selectedCourse, state.currentLectureIndex + 1);
  }
}

function prevLecture() {
  if (!state.selectedCourse) return;
  if (state.currentLectureIndex > 1) {
    playCourse(state.selectedCourse, state.currentLectureIndex - 1);
  }
}

// Open Current in Browser
function openCurrentInBrowser() {
  if (!state.selectedCourse) return;
  const pid = state.selectedCourse.playlistId || "";
  const idx = state.currentLectureIndex || 1;
  const vid = state.selectedCourse.firstVideoId || "";
  let url = `https://www.youtube.com/watch?list=${pid}&index=${idx}`;
  if (idx === 1 && vid) {
    url = `https://www.youtube.com/watch?v=${vid}&list=${pid}&index=1`;
  }
  openExternal(url);
}

// Create Rich Course Card with 16:9 Thumbnail (Matches Android item_course_card.xml)
function createCourseCard(course) {
  const code = getCourseCode(course);
  const title = getCleanTitle(course);
  const dept = getDepartment(course);
  const countText = course.videoCountText || `${course.videoCount || 45} lectures`;
  const isSaved = state.savedCodes.has(code);
  const thumbUrl = getCourseThumbnail(course);

  const card = document.createElement("div");
  card.className = "course-card";
  card.innerHTML = `
    <div class="card-thumb-wrapper">
      <img src="${thumbUrl}" alt="${title}" class="card-thumb-img" onerror="this.src='icons/icon.png'">
      <span class="card-duration-badge">${countText}</span>
      <div class="card-play-overlay">
        <div class="card-play-icon">PLAY</div>
      </div>
    </div>
    <div class="card-content">
      <div>
        <div class="card-top">
          <span class="card-code">${code}</span>
          <button class="btn-bookmark ${isSaved ? "saved" : ""}" data-bookmark-code="${code}" title="${isSaved ? "Saved to Bookmarks" : "Save Course"}">
            ${isSaved ? "Saved" : "Save"}
          </button>
        </div>
        <h4 class="card-title">${title}</h4>
        <p class="card-dept">${dept}</p>
      </div>
      <div class="card-bottom">
        <span class="card-count">${countText}</span>
        <button class="btn btn-primary btn-sm watch-btn">Watch</button>
      </div>
    </div>
  `;

  card.querySelector(".btn-bookmark").onclick = (e) => {
    e.stopPropagation();
    toggleBookmark(code);
  };

  card.querySelector(".watch-btn").onclick = (e) => {
    e.stopPropagation();
    playCourse(course, 1);
  };

  card.onclick = () => {
    playCourse(course, 1);
  };

  return card;
}

// Create Compact Course Card for Learn Tab Carousel (Matches Android item_compact_course_card.xml)
function createCompactCourseCard(course) {
  const code = getCourseCode(course);
  const title = getCleanTitle(course);
  const countText = `${course.videoCount || 45} lectures`;
  const thumbUrl = getCourseThumbnail(course);

  const card = document.createElement("div");
  card.className = "compact-course-card";
  card.innerHTML = `
    <div class="compact-thumb-box">
      <img src="${thumbUrl}" alt="${title}" class="compact-thumb-img" onerror="this.src='icons/icon.png'">
    </div>
    <div class="compact-card-body">
      <span class="compact-code">${code}</span>
      <h5 class="compact-title">${title}</h5>
      <span class="compact-lec-count">${countText}</span>
    </div>
  `;

  card.onclick = () => {
    playCourse(course, 1);
  };

  return card;
}

// Render Learn View (Dual State: Active Learning vs Empty State, matches Android HomeFragment)
function renderHomeView() {
  const activeDashboard = document.getElementById("home-active-dashboard");
  const emptyDashboard = document.getElementById("home-empty-dashboard");

  // Determine active enrolled/saved courses
  const enrolledList = state.courses.filter(c => state.enrolledCodes.has(getCourseCode(c)) || state.savedCodes.has(getCourseCode(c)));

  if (enrolledList.length === 0) {
    // STATE B: New User Empty State
    activeDashboard.classList.add("hidden");
    emptyDashboard.classList.remove("hidden");

    const emptyFeaturedGrid = document.getElementById("home-empty-featured-grid");
    emptyFeaturedGrid.innerHTML = "";
    state.courses.slice(0, 6).forEach(c => {
      emptyFeaturedGrid.appendChild(createCourseCard(c));
    });

    document.getElementById("stat-courses").textContent = `${state.courses.length}+`;
    document.getElementById("stat-depts").textContent = `${Math.max(1, state.departments.length - 1)}`;
  } else {
    // STATE A: Active Learning Dashboard
    emptyDashboard.classList.add("hidden");
    activeDashboard.classList.remove("hidden");

    // 1. Hero Continue Learning Card
    let heroCourse = null;
    let heroLecIdx = 1;
    if (state.lastWatched) {
      heroCourse = state.courses.find(c => getCourseCode(c) === state.lastWatched.code);
      heroLecIdx = state.lastWatched.lectureIndex || 1;
    }
    if (!heroCourse) {
      heroCourse = enrolledList[0];
      heroLecIdx = 1;
    }

    const heroCode = getCourseCode(heroCourse);
    const heroTitle = getCleanTitle(heroCourse);
    const heroDept = getDepartment(heroCourse);
    const heroTotal = Math.max(1, heroCourse.videoCount || 45);
    const heroProgressPct = Math.min(100, Math.round((heroLecIdx / heroTotal) * 100));

    document.getElementById("hero-course-thumb").src = getCourseThumbnail(heroCourse);
    document.getElementById("hero-course-duration").textContent = `${heroTotal} Lectures`;
    document.getElementById("hero-course-dept").textContent = `${heroDept} • Virtual University`;
    document.getElementById("hero-course-title").textContent = `${heroCode} - ${heroTitle}`;
    document.getElementById("hero-course-progress").textContent = `Progress: Lecture ${heroLecIdx} of ${heroTotal}`;
    document.getElementById("hero-progress-bar").style.width = `${heroProgressPct}%`;
    document.getElementById("hero-resume-lec-title").textContent = `Lecture ${String(heroLecIdx).padStart(2, "0")} - ${heroTitle}`;

    // Click on hero card or resume ribbon
    const handleHeroPlay = () => playCourse(heroCourse, heroLecIdx);
    document.getElementById("hero-course-card").onclick = handleHeroPlay;
    document.getElementById("btn-hero-resume").onclick = (e) => {
      e.stopPropagation();
      handleHeroPlay();
    };

    // 2. Horizontal Carousel of Enrolled Courses
    const carouselEl = document.getElementById("home-enrolled-carousel");
    carouselEl.innerHTML = "";
    enrolledList.forEach(c => {
      carouselEl.appendChild(createCompactCourseCard(c));
    });
    document.getElementById("home-my-courses-title").textContent = `My Courses (${enrolledList.length})`;

    // 3. Recommended Courses Section
    const featuredGrid = document.getElementById("home-featured-grid");
    featuredGrid.innerHTML = "";
    const nonEnrolled = state.courses.filter(c => !state.enrolledCodes.has(getCourseCode(c))).slice(0, 6);
    const displayList = nonEnrolled.length > 0 ? nonEnrolled : state.courses.slice(0, 6);
    displayList.forEach(c => {
      featuredGrid.appendChild(createCourseCard(c));
    });
  }
}

// Render Explore Courses View
function renderCoursesView() {
  const container = document.getElementById("courses-grid");
  const countText = document.getElementById("courses-count-text");
  const query = state.searchQuery.trim().toLowerCase();

  const filtered = state.courses.filter(c => {
    const deptMatch = state.selectedDept === "All" || getDepartment(c).toLowerCase() === state.selectedDept.toLowerCase();
    const code = getCourseCode(c).toLowerCase();
    const title = (c.title || "").toLowerCase();
    const dept = getDepartment(c).toLowerCase();
    const searchMatch = !query || code.includes(query) || title.includes(query) || dept.includes(query);
    return deptMatch && searchMatch;
  });

  countText.textContent = `Showing ${filtered.length} courses`;
  container.innerHTML = "";

  filtered.forEach(c => {
    container.appendChild(createCourseCard(c));
  });
}

// Render Department Pills
function renderDeptPills() {
  const bar = document.getElementById("dept-pills-bar");
  bar.innerHTML = "";

  state.departments.forEach(dept => {
    const pill = document.createElement("button");
    pill.className = `dept-pill ${state.selectedDept === dept ? "active" : ""}`;
    pill.textContent = dept;
    pill.onclick = () => {
      state.selectedDept = dept;
      document.querySelectorAll(".dept-pill").forEach(p => p.classList.remove("active"));
      pill.classList.add("active");
      renderCoursesView();
    };
    bar.appendChild(pill);
  });
}

// Render My Learning View (Matches Android SavedFragment: My Courses, Bookmarks, and Watch History)
function renderSavedView() {
  const contentWrapper = document.getElementById("saved-content-wrapper");
  const emptyState = document.getElementById("saved-empty-state");

  const myCoursesList = state.courses.filter(c => state.enrolledCodes.has(getCourseCode(c)));
  const bookmarkedList = state.courses.filter(c => state.savedCodes.has(getCourseCode(c)));
  const historyList = state.watchHistory;

  const totalItems = myCoursesList.length + bookmarkedList.length + historyList.length;

  if (totalItems === 0) {
    contentWrapper.classList.add("hidden");
    emptyState.classList.remove("hidden");
    return;
  }

  contentWrapper.classList.remove("hidden");
  emptyState.classList.add("hidden");

  // Update Section Badges
  document.getElementById("count-my-courses").textContent = myCoursesList.length;
  document.getElementById("count-bookmarked").textContent = bookmarkedList.length;
  document.getElementById("count-history").textContent = historyList.length;

  // Filter sections by subtab
  const secMyCourses = document.getElementById("section-my-courses");
  const secBookmarked = document.getElementById("section-bookmarked");
  const secHistory = document.getElementById("section-history");

  const sub = state.savedSubtab;
  secMyCourses.style.display = (sub === "all" || sub === "courses") && myCoursesList.length > 0 ? "flex" : "none";
  secBookmarked.style.display = (sub === "all" || sub === "bookmarks") && bookmarkedList.length > 0 ? "flex" : "none";
  secHistory.style.display = (sub === "all" || sub === "history") && historyList.length > 0 ? "flex" : "none";

  // 1. Render My Courses Grid
  const gridMyCourses = document.getElementById("grid-my-courses");
  gridMyCourses.innerHTML = "";
  myCoursesList.forEach(c => {
    gridMyCourses.appendChild(createCourseCard(c));
  });

  // 2. Render Bookmarked Courses Grid
  const gridBookmarked = document.getElementById("grid-bookmarked");
  gridBookmarked.innerHTML = "";
  bookmarkedList.forEach(c => {
    gridBookmarked.appendChild(createCourseCard(c));
  });

  // 3. Render Watch History List
  const listHistory = document.getElementById("list-watch-history");
  listHistory.innerHTML = "";
  historyList.forEach(item => {
    const historyCard = document.createElement("div");
    historyCard.className = "history-item";
    historyCard.innerHTML = `
      <div class="history-left">
        <div class="history-thumb-box">
          <img src="${item.thumbnailUrl || 'icons/icon.png'}" alt="${item.courseTitle}" class="history-thumb-img" onerror="this.src='icons/icon.png'">
          <span class="history-lec-badge">Lec ${String(item.lectureIndex).padStart(2, "0")}</span>
        </div>
        <div class="history-meta">
          <span class="history-code">${item.courseCode}</span>
          <h4 class="history-title">${item.lectureTitle}</h4>
          <span class="history-time">${formatRelativeTime(item.timestamp)}</span>
        </div>
      </div>
      <button class="btn btn-primary btn-sm">Resume</button>
    `;

    historyCard.onclick = () => {
      const course = state.courses.find(c => getCourseCode(c) === item.courseCode || c.playlistId === item.playlistId);
      if (course) {
        playCourse(course, item.lectureIndex);
      }
    };

    listHistory.appendChild(historyCard);
  });
}

// Render Resources View (Merged Handouts + Links + Tools, matches Android ResourcesActivity)
function renderResourcesView() {
  const query = state.resourceQuery.trim().toLowerCase();

  // 1. Handouts Tab
  const handoutsGrid = document.getElementById("handouts-grid");
  handoutsGrid.innerHTML = "";

  const filteredCourses = state.courses.filter(c => {
    const code = getCourseCode(c).toLowerCase();
    const title = getCleanTitle(c).toLowerCase();
    const dept = getDepartment(c).toLowerCase();
    return !query || code.includes(query) || title.includes(query) || dept.includes(query);
  });

  filteredCourses.forEach(c => {
    const code = getCourseCode(c);
    const title = getCleanTitle(c);
    const dept = getDepartment(c);

    const card = document.createElement("div");
    card.className = "resource-card";
    card.innerHTML = `
      <div>
        <span class="chip chip-primary">${code}</span>
        <h4 class="resource-title" style="margin-top: 10px;">${title}</h4>
        <p class="resource-desc">${dept}</p>
      </div>
      <button class="btn btn-secondary btn-sm download-btn">Download Handout PDF</button>
    `;
    card.querySelector(".download-btn").onclick = () => {
      openExternal(`https://www.google.com/search?q=VU+${code}+handouts+filetype:pdf`);
    };
    handoutsGrid.appendChild(card);
  });

  // 2. VU Portals Tab
  const portalsGrid = document.getElementById("portals-grid");
  portalsGrid.innerHTML = "";

  const filteredPortals = officialLinks.filter(link => {
    return !query || link.title.toLowerCase().includes(query) || link.description.toLowerCase().includes(query);
  });

  filteredPortals.forEach(link => {
    const card = document.createElement("div");
    card.className = "resource-card";
    card.innerHTML = `
      <div>
        <h4 class="resource-title">${link.title}</h4>
        <p class="resource-desc" style="margin-top: 6px;">${link.description}</p>
      </div>
      <button class="btn btn-outline btn-sm open-btn">Launch Portal &rarr;</button>
    `;
    card.querySelector(".open-btn").onclick = () => {
      openExternal(link.url);
    };
    portalsGrid.appendChild(card);
  });

  // 3. Academic Tools Tab
  const toolsGrid = document.getElementById("tools-grid");
  toolsGrid.innerHTML = "";

  const filteredTools = academicTools.filter(tool => {
    return !query || tool.title.toLowerCase().includes(query) || tool.description.toLowerCase().includes(query);
  });

  filteredTools.forEach(tool => {
    const card = document.createElement("div");
    card.className = "resource-card";
    card.innerHTML = `
      <div>
        <h4 class="resource-title">${tool.title}</h4>
        <p class="resource-desc" style="margin-top: 6px;">${tool.description}</p>
      </div>
      <button class="btn btn-outline btn-sm open-btn">Open Tool &rarr;</button>
    `;
    card.querySelector(".open-btn").onclick = () => {
      openExternal(tool.url);
    };
    toolsGrid.appendChild(card);
  });

  // Show active pane
  document.querySelectorAll(".res-tab-pane").forEach(pane => pane.classList.add("hidden"));
  const activePane = document.getElementById(`resource-tab-${state.resourceSubtab}`);
  if (activePane) {
    activePane.classList.remove("hidden");
    activePane.classList.add("active");
  }
}

// Load Courses Data from Tauri Backend or Local Assets
async function loadCoursesData() {
  try {
    const invoke = getTauriInvoke();
    if (invoke) {
      const raw = await invoke("get_courses");
      state.courses = JSON.parse(raw);
    } else {
      const resp = await fetch("assets/courses.json");
      state.courses = await resp.json();
    }
  } catch (e) {
    console.error("Error loading courses:", e);
    try {
      const resp = await fetch("assets/courses.json");
      state.courses = await resp.json();
    } catch (err) {
      state.courses = [];
    }
  }

  // Extract unique departments
  const deptSet = new Set();
  state.courses.forEach(c => {
    const d = getDepartment(c);
    if (d && d !== "General") deptSet.add(d);
  });
  state.departments = ["All", ...Array.from(deptSet).sort()];

  // Render initial views
  renderDeptPills();
  renderCoursesView();
  renderHomeView();
  renderResourcesView();
  loadUserData();
}

// Initialize Application Events
function initApp() {
  initTheme();
  loadUserData();
  initSidebarState();
  initPlaylistState();

  // Navigation Click Handlers
  document.querySelectorAll(".nav-item").forEach(item => {
    item.onclick = () => {
      switchTab(item.dataset.tab);
    };
  });

  // Sidebar Collapse Button
  const btnSidebarToggle = document.getElementById("btn-sidebar-toggle");
  if (btnSidebarToggle) {
    btnSidebarToggle.onclick = toggleSidebar;
  }

  // Explore Courses Search
  const searchInput = document.getElementById("course-search-input");
  const clearBtn = document.getElementById("search-clear-btn");
  searchInput.oninput = (e) => {
    state.searchQuery = e.target.value;
    if (state.searchQuery.length > 0) {
      clearBtn.classList.remove("hidden");
    } else {
      clearBtn.classList.add("hidden");
    }
    renderCoursesView();
  };
  clearBtn.onclick = () => {
    searchInput.value = "";
    state.searchQuery = "";
    clearBtn.classList.add("hidden");
    renderCoursesView();
  };

  // Resources Search
  const resSearch = document.getElementById("resource-search-input");
  const resClear = document.getElementById("resource-search-clear");
  resSearch.oninput = (e) => {
    state.resourceQuery = e.target.value;
    if (state.resourceQuery.length > 0) {
      resClear.classList.remove("hidden");
    } else {
      resClear.classList.add("hidden");
    }
    renderResourcesView();
  };
  resClear.onclick = () => {
    resSearch.value = "";
    state.resourceQuery = "";
    resClear.classList.add("hidden");
    renderResourcesView();
  };

  // Resources Sub-tabs Switcher
  document.querySelectorAll(".resource-tab-btn").forEach(btn => {
    btn.onclick = () => {
      document.querySelectorAll(".resource-tab-btn").forEach(b => b.classList.remove("active"));
      btn.classList.add("active");
      state.resourceSubtab = btn.dataset.resTab;
      renderResourcesView();
    };
  });

  // My Learning Segmented Filter Pills
  document.querySelectorAll(".segment-pill").forEach(pill => {
    pill.onclick = () => {
      document.querySelectorAll(".segment-pill").forEach(p => p.classList.remove("active"));
      pill.classList.add("active");
      state.savedSubtab = pill.dataset.subtab;
      renderSavedView();
    };
  });

  // Clear Watch History Button
  const btnClearHist = document.getElementById("btn-clear-history");
  if (btnClearHist) {
    btnClearHist.onclick = clearWatchHistory;
  }

  // Theme Toggle Button
  document.getElementById("theme-toggle").onclick = toggleTheme;

  // Player Toolbar Controls
  document.getElementById("btn-play-video").onclick = () => {
    if (state.selectedCourse) {
      playCourse(state.selectedCourse, state.currentLectureIndex);
    }
  };
  document.getElementById("btn-prev-lec").onclick = prevLecture;
  document.getElementById("btn-next-lec").onclick = nextLecture;
  document.getElementById("btn-open-browser").onclick = openCurrentInBrowser;

  // Explicit Fullscreen Button & Listeners
  const btnFs = document.getElementById("btn-fullscreen");
  if (btnFs) {
    btnFs.onclick = toggleFullscreen;
  }
  document.addEventListener("fullscreenchange", handleFullscreenChange);
  document.addEventListener("webkitfullscreenchange", handleFullscreenChange);
  document.addEventListener("mozfullscreenchange", handleFullscreenChange);
  document.addEventListener("MSFullscreenChange", handleFullscreenChange);

  // Speed Stepper Controls ([-] 1.0x [+])
  const btnSpeedDown = document.getElementById("btn-speed-down");
  const btnSpeedUp = document.getElementById("btn-speed-up");
  const speedDisplay = document.getElementById("speed-display");
  if (btnSpeedDown) btnSpeedDown.onclick = decreaseSpeed;
  if (btnSpeedUp) btnSpeedUp.onclick = increaseSpeed;
  if (speedDisplay) speedDisplay.onclick = cycleSpeed;

  // Playlist Collapsible Controls
  const btnTogglePlaylist = document.getElementById("btn-toggle-playlist");
  const btnClosePlaylist = document.getElementById("btn-close-playlist");
  if (btnTogglePlaylist) btnTogglePlaylist.onclick = () => togglePlaylist();
  if (btnClosePlaylist) btnClosePlaylist.onclick = () => togglePlaylist(true);

  // Load Data
  loadCoursesData();
}

// Start Application on DOM Ready
document.addEventListener("DOMContentLoaded", initApp);
