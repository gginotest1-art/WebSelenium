package com.webselenium.pages;

import com.webselenium.base.BasePage;
import com.webselenium.helpers.LogUtils;
import com.webselenium.models.GameCheckResult;
import com.webselenium.models.GameInventoryItem;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameLobbyPage extends BasePage {

    private static final By LOBBY_TABS = By.xpath("//*[@id='mega-menu-full-image']//li");
    private static final By PROVIDER_DROPDOWN = By.xpath("//li[contains(@class,'dropdown-item')]");
    private static final By PROVIDER_NAME = By.xpath("//div[contains(@class,'dropdown-item__label')]");
    private static final By GAME_ITEMS = By.cssSelector("div.game-item");
    private static final By GAME_IFRAME = By.tagName("iframe");
    private static final By ERROR_MODAL = By.cssSelector("div.modal-overlay");
    private static final By ERROR_MODAL_TEXT = By.cssSelector("div.modal-alert-content");
    private static final By ERROR_MODAL_DISMISS = By.cssSelector("button.modal-button");
    // Button pagination text format: "Xem thêm (current / total)" — match cả 2 phần để loại nút footer
    private static final By SHOW_MORE_BTN = By.xpath(
            "//button[contains(normalize-space(.), 'Xem thêm (') and contains(normalize-space(.), ' / ')]"
    );
    // Site có lobby ~2145 game, ~20 game/click → ~108 click cần thiết
    private static final int MAX_SHOW_MORE_CLICKS = 250;

    private static final Duration NEW_WINDOW_TIMEOUT = Duration.ofSeconds(8);
    private static final Duration IFRAME_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration GAME_LOAD_TIMEOUT = Duration.ofSeconds(20);
    private static final int MIN_IFRAME_DIMENSION = 50;

    private static final List<String> ERROR_KEYWORDS = List.of(
            "đang bảo trì", "site under maintenance", "service unavailable",
            "page not found", "404 not found", "không tìm thấy",
            "không thể tải", "không thể kết nối", "cannot be reached",
            "failed to load", "this site can't be reached",
            "internal server error", "503", "502"
    );

    public GameLobbyPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Inventory CỔNG GAME — lobby chứa tất cả game. Cách nhanh nhất để lấy danh sách
     * đầy đủ game của site mà không cần duyệt qua các lobby khác.
     */
    public List<GameInventoryItem> inventoryGamePortal() {
        return inventoryGamePortal("CỔNG GAME");
    }

    public List<GameInventoryItem> inventoryGamePortal(String portalName) {
        List<GameInventoryItem> items = new ArrayList<>();
        Duration original = saveAndDisableImplicitWait();
        try {
            ensureValidWindow();

            WebElement portal = findLobbyByName(portalName);
            if (portal == null) {
                LogUtils.error("Không tìm thấy lobby '" + portalName + "'");
                return items;
            }
            LogUtils.info("Opening portal lobby: " + portalName);
            try {
                scrollIntoView(portal);
                portal.click();
                pause(500);
            } catch (Exception e) {
                LogUtils.error("Cannot click portal: " + e.getMessage(), e);
                return items;
            }

            waitForGameTilesOrTimeout(3000);

            boolean hasProvider = !driver.findElements(PROVIDER_DROPDOWN).isEmpty();
            if (hasProvider) {
                int providerCount = driver.findElements(PROVIDER_NAME).size();
                LogUtils.info("Portal '" + portalName + "' has " + providerCount + " providers");
                for (int p = 0; p < providerCount; p++) {
                    // re-open portal trước mỗi provider
                    WebElement portalRe = findLobbyByName(portalName);
                    if (portalRe != null) {
                        try { scrollIntoView(portalRe); portalRe.click(); pause(400); }
                        catch (Exception ignored) {}
                    }
                    String providerName = openProviderByIndex(p);
                    if (providerName == null) continue;
                    collectInventoryGames(items, portalName, providerName);
                }
            } else {
                collectInventoryGames(items, portalName, "-");
            }
            LogUtils.info("Portal inventory total: " + items.size() + " games");
            return items;
        } finally {
            restoreImplicitWait(original);
        }
    }

    /**
     * Sample N game ngẫu nhiên mỗi provider, click vào và verify chơi được hay không.
     * - Open CỔNG GAME + load full inventory (clicking Xem thêm)
     * - Group by provider, random sample
     * - Mỗi game: click qua "Chơi ngay" button hoặc tile → đợi 6s
     *   - Modal error (div.modal-overlay) → FAIL với text từ modal-alert-content
     *   - New tab → PASS (NEW_TAB)
     *   - Iframe game → PASS (IFRAME)
     *   - Không gì → FAIL (NONE)
     * - Dismiss modal nếu còn, recover before next sample
     */
    public List<GameCheckResult> sampleVerifyGames(int perProvider) {
        final int EXPAND_CLICKS = 30; // ~600 tiles, cover ~10 providers
        List<GameCheckResult> results = new ArrayList<>();
        Duration originalImplicit = saveAndDisableImplicitWait();
        String mainWindow;
        try { mainWindow = driver.getWindowHandle(); }
        catch (Exception e) { return results; }

        try {
            openPortalAndExpand(EXPAND_CLICKS);
            List<Map<String, String>> allTiles = extractAllGameTilesViaJS();
            LogUtils.info("Snapshot " + allTiles.size() + " tiles để chọn sample");

            // Group theo provider
            Map<String, List<Map<String, String>>> byProvider = new LinkedHashMap<>();
            for (Map<String, String> t : allTiles) {
                String p = t.getOrDefault("provider", "-");
                if (p == null || p.isBlank()) p = "-";
                byProvider.computeIfAbsent(p, k -> new ArrayList<>()).add(t);
            }

            // Sample N per provider — lưu (name, provider) pairs
            Random rng = new Random(42);
            List<String[]> samples = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, String>>> e : byProvider.entrySet()) {
                List<Map<String, String>> shuffled = new ArrayList<>(e.getValue());
                Collections.shuffle(shuffled, rng);
                int n = Math.min(perProvider, shuffled.size());
                for (int j = 0; j < n; j++) {
                    samples.add(new String[]{shuffled.get(j).getOrDefault("name", ""), e.getKey()});
                }
            }
            LogUtils.info("Sample " + samples.size() + " game (~" + perProvider + "/provider × " + byProvider.size() + " providers)");

            // Verify từng sample: tìm tile theo tên (KHÔNG dùng index để robust với recovery)
            for (int s = 0; s < samples.size(); s++) {
                String name = samples.get(s)[0];
                String provider = samples.get(s)[1];

                // Đảm bảo đang ở portal (recover nếu URL thay đổi/state dropped)
                if (!isOnPortal()) {
                    LogUtils.info("Không ở portal, recover...");
                    recoverPortal(EXPAND_CLICKS);
                }

                GameCheckResult r = verifyGameByName(name, provider, mainWindow);
                results.add(r);
                LogUtils.info((s + 1) + "/" + samples.size() + " " + r.toString());

                dismissErrorModalIfAny();

                // Sau iframe: recover bắt buộc
                if (r.mode == GameCheckResult.OpenMode.IFRAME) {
                    recoverPortal(EXPAND_CLICKS);
                }
                pause(150);
            }
            return results;
        } finally {
            restoreImplicitWait(originalImplicit);
        }
    }

    private boolean isOnPortal() {
        try {
            int count = driver.findElements(GAME_ITEMS).size();
            return count >= 20; // có ít nhất 20 tile coi như đang ở portal
        } catch (Exception e) {
            return false;
        }
    }

    private GameCheckResult verifyGameByName(String name, String provider, String mainWindow) {
        // JS: tìm tile theo tên (case-insensitive), click button "Chơi ngay" hoặc tile
        String findClickJs =
                "const target = " + jsString(name.trim().toLowerCase()) + ";" +
                "const tiles = document.querySelectorAll('div.game-item');" +
                "for (const t of tiles) {" +
                "  const titleEl = t.querySelector('p.game-item__title');" +
                "  const title = titleEl ? (titleEl.innerText || titleEl.textContent || '').trim().toLowerCase() : '';" +
                "  if (title === target) {" +
                "    t.scrollIntoView({block:'center'});" +
                "    let btn = t.querySelector('button.bg-rose-600');" +
                "    if (!btn) btn = t.querySelector('button');" +
                "    if (btn) { btn.click(); return 'CLICKED_BTN'; }" +
                "    t.click(); return 'CLICKED_TILE';" +
                "  }" +
                "}" +
                "return 'NOT_FOUND';";

        Set<String> beforeHandles;
        try { beforeHandles = new HashSet<>(driver.getWindowHandles()); }
        catch (Exception e) {
            return new GameCheckResult("CỔNG GAME", provider, name, false,
                    GameCheckResult.OpenMode.NONE, "getWindowHandles failed");
        }

        Object clickResult;
        try { clickResult = ((JavascriptExecutor) driver).executeScript(findClickJs); }
        catch (Exception e) {
            return new GameCheckResult("CỔNG GAME", provider, name, false,
                    GameCheckResult.OpenMode.NONE, "JS click failed: " + e.getMessage());
        }
        if ("NOT_FOUND".equals(String.valueOf(clickResult))) {
            return new GameCheckResult("CỔNG GAME", provider, name, false,
                    GameCheckResult.OpenMode.NONE, "Tile name không tìm thấy trong DOM hiện tại");
        }

        long deadline = System.currentTimeMillis() + 6000;
        while (System.currentTimeMillis() < deadline) {
            List<WebElement> modals = driver.findElements(ERROR_MODAL);
            if (!modals.isEmpty()) {
                String errorText = "(no text)";
                try {
                    List<WebElement> tx = modals.get(0).findElements(ERROR_MODAL_TEXT);
                    if (!tx.isEmpty()) errorText = tx.get(0).getText().trim();
                } catch (Exception ignored) {}
                try {
                    List<WebElement> btn = modals.get(0).findElements(ERROR_MODAL_DISMISS);
                    if (!btn.isEmpty()) ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn.get(0));
                } catch (Exception ignored) {}
                pause(300);
                return new GameCheckResult("CỔNG GAME", provider, name, false,
                        GameCheckResult.OpenMode.NONE, "Modal: " + errorText);
            }
            Set<String> current;
            try { current = new HashSet<>(driver.getWindowHandles()); }
            catch (Exception e) { break; }
            if (current.size() > beforeHandles.size()) {
                Set<String> diff = new HashSet<>(current);
                diff.removeAll(beforeHandles);
                String newTab = diff.iterator().next();
                String url = "";
                try { driver.switchTo().window(newTab); url = driver.getCurrentUrl(); }
                catch (Exception ignored) {}
                finally {
                    try { driver.close(); } catch (Exception ignored) {}
                    switchToSurvivingWindow(mainWindow);
                }
                boolean ok = isUrlPlayable(url);
                return new GameCheckResult("CỔNG GAME", provider, name, ok,
                        GameCheckResult.OpenMode.NEW_TAB, ok ? "url=" + url : "Invalid URL: " + url);
            }
            WebElement iframe = pickLargestIframe();
            if (iframe != null) {
                String src = "";
                try { src = iframe.getAttribute("src"); } catch (Exception ignored) {}
                if (src != null && !src.isBlank() && !src.equals("about:blank")) {
                    return new GameCheckResult("CỔNG GAME", provider, name, true,
                            GameCheckResult.OpenMode.IFRAME, "src=" + src);
                }
            }
            pause(150);
        }
        return new GameCheckResult("CỔNG GAME", provider, name, false,
                GameCheckResult.OpenMode.NONE, "No outcome after 6s");
    }

    private String jsString(String s) {
        if (s == null) return "''";
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private void openPortalAndExpand(int maxClicks) {
        ensureValidWindow();
        String countJs =
                "return Array.from(document.querySelectorAll('button')).filter(b => {" +
                "  const t = ((b.innerText || b.textContent || '').trim()).toLowerCase();" +
                "  return t.includes('xem th');" +
                "}).length;";

        boolean btnAppeared = false;
        int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            WebElement portal = findLobbyByName("CỔNG GAME");
            if (portal == null) {
                LogUtils.warn("Attempt " + attempt + ": Không tìm thấy CỔNG GAME tab, đợi rồi retry...");
                pause(2000);
                continue;
            }
            try {
                scrollIntoView(portal);
                portal.click();
                LogUtils.info("Attempt " + attempt + ": Clicked CỔNG GAME");
            } catch (Exception e) {
                LogUtils.warn("Attempt " + attempt + ": Click portal failed: " + e.getMessage());
                pause(1000);
                continue;
            }
            // Đợi SPA transition: Xem thêm button xuất hiện (max 15s/attempt)
            long deadline = System.currentTimeMillis() + 15000;
            while (System.currentTimeMillis() < deadline) {
                try {
                    Object n = ((JavascriptExecutor) driver).executeScript(countJs);
                    if (n != null && Long.parseLong(n.toString()) > 0) {
                        btnAppeared = true;
                        break;
                    }
                } catch (Exception ignored) {}
                pause(300);
            }
            if (btnAppeared) {
                LogUtils.info("Attempt " + attempt + ": Xem thêm button xuất hiện ✓");
                break;
            }
            LogUtils.warn("Attempt " + attempt + ": Button chưa xuất hiện sau 15s, retry...");
        }
        if (!btnAppeared) {
            LogUtils.error("Sau " + maxAttempts + " attempt, Xem thêm button vẫn không xuất hiện — bỏ expand");
            return;
        }
        pause(500);
        int clicks = expandShowMoreLimited(maxClicks);
        LogUtils.info("Pre-expand done: " + clicks + " clicks, " + driver.findElements(GAME_ITEMS).size() + " tiles");
    }

    private void recoverPortal(int maxClicks) {
        LogUtils.info("Recovering portal state (" + maxClicks + " Xem thêm clicks)...");
        try {
            driver.get(com.webselenium.helpers.ConfigReader.getBaseUrl());
            pause(1500);
        } catch (Exception e) {
            LogUtils.warn("Recover navigate failed: " + e.getMessage());
        }
        openPortalAndExpand(maxClicks);
    }

    private int expandShowMoreLimited(int maxClicks) {
        int clicks = 0;
        String findButtonJs =
                "function find() {" +
                "  const btns = Array.from(document.querySelectorAll('button')).filter(b => {" +
                "    const t = ((b.innerText || b.textContent || '').trim()).toLowerCase();" +
                "    return t.includes('xem th');" +
                "  });" +
                "  return btns[0] || null;" +
                "}" +
                "const btn = find();" +
                "if (!btn) return JSON.stringify({status:'none'});" +
                "return JSON.stringify({" +
                "  status: btn.disabled ? 'disabled' : 'enabled'," +
                "  text: (btn.innerText || btn.textContent || '').trim()" +
                "});";
        String clickButtonJs =
                "const btns = Array.from(document.querySelectorAll('button')).filter(b => {" +
                "  const t = ((b.innerText || b.textContent || '').trim()).toLowerCase();" +
                "  return t.includes('xem th');" +
                "});" +
                "if (btns.length === 0) return null;" +
                "const btn = btns[0];" +
                "if (btn.disabled) return 'DISABLED';" +
                "btn.scrollIntoView({block:'center'});" +
                "btn.click();" +
                "return (btn.innerText || btn.textContent || '').trim();";
        for (int i = 0; i < maxClicks; i++) {
            int before = driver.findElements(GAME_ITEMS).size();
            if (before == 0) break;
            scrollToBottom();
            if (!waitForButtonEnabled(findButtonJs, 10000)) break;
            pause(300);
            Object clicked;
            try { clicked = ((JavascriptExecutor) driver).executeScript(clickButtonJs); }
            catch (Exception e) { break; }
            if (clicked == null) break;
            if ("DISABLED".equals(clicked.toString())) { pause(500); continue; }
            clicks++;
            waitForGameCountIncrease(before, 4000);
        }
        return clicks;
    }

    private GameCheckResult verifyGameByTileIndex(int idx, String name, String provider, String mainWindow) {
        Set<String> beforeHandles;
        try { beforeHandles = new HashSet<>(driver.getWindowHandles()); }
        catch (Exception e) {
            return new GameCheckResult("CỔNG GAME", provider, name, false,
                    GameCheckResult.OpenMode.NONE, "getWindowHandles failed: " + e.getMessage());
        }

        // JS click: ưu tiên button "Chơi ngay" trong tile, fallback click tile
        String clickJs =
                "const tiles = document.querySelectorAll('div.game-item');" +
                "if (tiles.length <= " + idx + ") return 'NO_TILE';" +
                "const tile = tiles[" + idx + "];" +
                "tile.scrollIntoView({block:'center'});" +
                "let btn = tile.querySelector('button.bg-rose-600');" +
                "if (!btn) btn = tile.querySelector('button');" +
                "if (btn) { btn.click(); return 'CLICKED_BTN'; }" +
                "tile.click(); return 'CLICKED_TILE';";

        Object clickResult;
        try {
            clickResult = ((JavascriptExecutor) driver).executeScript(clickJs);
        } catch (Exception e) {
            return new GameCheckResult("CỔNG GAME", provider, name, false,
                    GameCheckResult.OpenMode.NONE, "JS click failed: " + e.getMessage());
        }
        if ("NO_TILE".equals(String.valueOf(clickResult))) {
            return new GameCheckResult("CỔNG GAME", provider, name, false,
                    GameCheckResult.OpenMode.NONE, "Tile index out of range");
        }

        long deadline = System.currentTimeMillis() + 6000;
        while (System.currentTimeMillis() < deadline) {
            // Error modal?
            List<WebElement> modals = driver.findElements(ERROR_MODAL);
            if (!modals.isEmpty()) {
                String errorText = "(no text)";
                try {
                    List<WebElement> tx = modals.get(0).findElements(ERROR_MODAL_TEXT);
                    if (!tx.isEmpty()) errorText = tx.get(0).getText().trim();
                } catch (Exception ignored) {}
                try {
                    List<WebElement> btn = modals.get(0).findElements(ERROR_MODAL_DISMISS);
                    if (!btn.isEmpty()) {
                        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn.get(0));
                    }
                } catch (Exception ignored) {}
                pause(300);
                return new GameCheckResult("CỔNG GAME", provider, name, false,
                        GameCheckResult.OpenMode.NONE, "Modal: " + errorText);
            }
            // New tab?
            Set<String> current;
            try { current = new HashSet<>(driver.getWindowHandles()); }
            catch (Exception e) { break; }
            if (current.size() > beforeHandles.size()) {
                Set<String> diff = new HashSet<>(current);
                diff.removeAll(beforeHandles);
                String newTab = diff.iterator().next();
                String url = "";
                try {
                    driver.switchTo().window(newTab);
                    url = driver.getCurrentUrl();
                } catch (Exception ignored) {
                } finally {
                    try { driver.close(); } catch (Exception ignored) {}
                    switchToSurvivingWindow(mainWindow);
                }
                boolean ok = isUrlPlayable(url);
                return new GameCheckResult("CỔNG GAME", provider, name, ok,
                        GameCheckResult.OpenMode.NEW_TAB,
                        ok ? "url=" + url : "Invalid URL: " + url);
            }
            // Iframe?
            WebElement iframe = pickLargestIframe();
            if (iframe != null) {
                String src = "";
                try { src = iframe.getAttribute("src"); } catch (Exception ignored) {}
                if (src != null && !src.isBlank() && !src.equals("about:blank")) {
                    try { driver.navigate().back(); pause(500); } catch (Exception ignored) {}
                    return new GameCheckResult("CỔNG GAME", provider, name, true,
                            GameCheckResult.OpenMode.IFRAME, "src=" + src);
                }
            }
            pause(150);
        }
        return new GameCheckResult("CỔNG GAME", provider, name, false,
                GameCheckResult.OpenMode.NONE, "No outcome after 6s");
    }

    private void dismissErrorModalIfAny() {
        try {
            List<WebElement> modals = driver.findElements(ERROR_MODAL);
            if (modals.isEmpty()) return;
            List<WebElement> btn = modals.get(0).findElements(ERROR_MODAL_DISMISS);
            if (!btn.isEmpty()) {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", btn.get(0));
                pause(300);
            }
        } catch (Exception ignored) {}
    }

    private WebElement findLobbyByName(String name) {
        if (name == null) return null;
        String target = name.trim();
        for (WebElement tab : driver.findElements(LOBBY_TABS)) {
            try {
                String text = safeText(tab);
                if (text.equalsIgnoreCase(target)) return tab;
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * Inventory mode: duyệt TẤT CẢ lobby/provider — chậm hơn nhưng đầy đủ nếu site
     * không có lobby tổng hợp như CỔNG GAME.
     */
    public List<GameInventoryItem> inventoryAllGames() {
        List<GameInventoryItem> items = new ArrayList<>();
        Duration originalImplicit = saveAndDisableImplicitWait();
        try {
            ensureValidWindow();

            int lobbyCount = driver.findElements(LOBBY_TABS).size();
            LogUtils.info("Inventory: found " + lobbyCount + " lobbies");

            for (int i = 0; i < lobbyCount; i++) {
                ensureValidWindow();
                String lobbyName = openLobbyByIndex(i);
                if (lobbyName == null) continue;

                boolean hasProvider = !driver.findElements(PROVIDER_DROPDOWN).isEmpty();
                if (hasProvider) {
                    int providerCount = driver.findElements(PROVIDER_NAME).size();
                    LogUtils.info("Lobby '" + lobbyName + "' has " + providerCount + " providers");
                    for (int p = 0; p < providerCount; p++) {
                        ensureValidWindow();
                        openLobbyByIndex(i);
                        String providerName = openProviderByIndex(p);
                        if (providerName == null) continue;
                        collectInventoryGames(items, lobbyName, providerName);
                    }
                } else {
                    LogUtils.info("Lobby '" + lobbyName + "' has no provider");
                    collectInventoryGames(items, lobbyName, "-");
                }
            }
            LogUtils.info("Inventory total: " + items.size() + " games");
            return items;
        } finally {
            restoreImplicitWait(originalImplicit);
        }
    }

    private Duration saveAndDisableImplicitWait() {
        try {
            Duration original = driver.manage().timeouts().getImplicitWaitTimeout();
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            LogUtils.info("Disabled implicit wait (was " + original.toSeconds() + "s)");
            return original;
        } catch (Exception e) {
            return Duration.ofSeconds(10);
        }
    }

    private void restoreImplicitWait(Duration value) {
        try {
            driver.manage().timeouts().implicitlyWait(value);
        } catch (Exception ignored) {}
    }

    private void collectInventoryGames(List<GameInventoryItem> items, String lobby, String providerFallback) {
        waitForGameTilesOrTimeout(2500);

        int initialCount = driver.findElements(GAME_ITEMS).size();
        if (initialCount == 0) {
            LogUtils.info("Inventory [" + lobby + " / " + providerFallback + "]: 0 games (skip show-more)");
            return;
        }

        int expanded = expandAllShowMore();
        if (expanded > 0) LogUtils.info("Clicked 'Xem thêm' " + expanded + " lần");

        // Batch lấy {name, provider} mỗi tile qua 1 lần JS
        List<Map<String, String>> tiles = extractAllGameTilesViaJS();
        LogUtils.info("Inventory [" + lobby + "]: " + tiles.size() + " games");

        for (Map<String, String> t : tiles) {
            String name = t.getOrDefault("name", "(unnamed)");
            String provider = t.getOrDefault("provider", "");
            if (provider == null || provider.isBlank()) provider = providerFallback;
            items.add(new GameInventoryItem(lobby, provider, name));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, String>> extractAllGameTilesViaJS() {
        String js =
                "const tiles = document.querySelectorAll('div.game-item');" +
                "const result = [];" +
                "tiles.forEach(t => {" +
                "  // GAME NAME: <p class='game-item__title'>\n" +
                "  let name = '';" +
                "  const titleEl = t.querySelector('p.game-item__title');" +
                "  if (titleEl) name = (titleEl.innerText || titleEl.textContent || '').trim();" +
                "  // PROVIDER: <p> thứ 2 trong info column (sau title)\n" +
                "  let provider = '';" +
                "  const allPs = t.querySelectorAll('p');" +
                "  for (const p of allPs) {" +
                "    if (p === titleEl) continue;" +
                "    if (p.classList.contains('game-item__title')) continue;" +
                "    const txt = (p.innerText || p.textContent || '').trim();" +
                "    if (txt && txt !== name) { provider = txt; break; }" +
                "  }" +
                "  // Fallback name từ img alt nếu vẫn rỗng\n" +
                "  if (!name) {" +
                "    const img = t.querySelector('img');" +
                "    if (img) name = (img.alt || img.title || '').trim();" +
                "  }" +
                "  result.push({name: name || '(unnamed)', provider: provider || ''});" +
                "});" +
                "return result;";
        try {
            Object raw = ((JavascriptExecutor) driver).executeScript(js);
            if (raw instanceof List) {
                List<Object> list = (List<Object>) raw;
                List<Map<String, String>> out = new ArrayList<>(list.size());
                for (Object o : list) {
                    if (o instanceof Map) {
                        Map<String, Object> m = (Map<String, Object>) o;
                        Map<String, String> entry = new HashMap<>();
                        for (Map.Entry<String, Object> e : m.entrySet()) {
                            entry.put(e.getKey(), e.getValue() == null ? "" : e.getValue().toString());
                        }
                        out.add(entry);
                    }
                }
                return out;
            }
        } catch (Exception e) {
            LogUtils.warn("JS extract failed: " + e.getMessage());
        }
        // Fallback: chỉ lấy name qua WebElement
        List<WebElement> games = driver.findElements(GAME_ITEMS);
        List<Map<String, String>> fallback = new ArrayList<>(games.size());
        for (WebElement g : games) {
            Map<String, String> m = new HashMap<>();
            try { m.put("name", extractGameName(g)); m.put("provider", ""); }
            catch (Exception e) { m.put("name", "(error)"); m.put("provider", ""); }
            fallback.add(m);
        }
        return fallback;
    }

    /**
     * Click nút "Xem thêm" ở cuối lobby liên tục đến khi không còn nút nữa.
     * Trả về số lần click đã thực hiện.
     */
    private int expandAllShowMore() {
        int clicks = 0;

        // Scroll xuống đáy + chờ để button "Xem thêm" render
        scrollToBottom();
        pause(800);

        // Diagnostic toàn diện
        String diagJs =
                "const result = {" +
                "  totalButtons: document.querySelectorAll('button').length," +
                "  iframes: document.querySelectorAll('iframe').length," +
                "  xemButtons: Array.from(document.querySelectorAll('button'))" +
                "    .filter(b => ((b.innerText || b.textContent || '').toLowerCase()).includes('xem'))" +
                "    .map(b => ((b.innerText || b.textContent || '').trim()).slice(0,80))," +
                "  buttonsContainingThem: Array.from(document.querySelectorAll('button'))" +
                "    .filter(b => ((b.innerText || b.textContent || '').toLowerCase()).includes('th'))" +
                "    .map(b => ((b.innerText || b.textContent || '').trim()).slice(0,80))" +
                "};" +
                "return JSON.stringify(result);";
        try {
            Object diag = ((JavascriptExecutor) driver).executeScript(diagJs);
            LogUtils.info("DIAG: " + diag);
        } catch (Exception ignored) {}

        // findButton: trả về object với status enabled/disabled/none, text
        String findButtonJs =
                "function find() {" +
                "  const btns = Array.from(document.querySelectorAll('button')).filter(b => {" +
                "    const t = ((b.innerText || b.textContent || '').trim()).toLowerCase();" +
                "    return t.includes('xem th');" +
                "  });" +
                "  return btns[0] || null;" +
                "}" +
                "const btn = find();" +
                "if (!btn) return JSON.stringify({status:'none'});" +
                "return JSON.stringify({" +
                "  status: btn.disabled ? 'disabled' : 'enabled'," +
                "  text: (btn.innerText || btn.textContent || '').trim()" +
                "});";

        // clickButton: chỉ click nếu enabled
        String clickButtonJs =
                "function find() {" +
                "  const btns = Array.from(document.querySelectorAll('button')).filter(b => {" +
                "    const t = ((b.innerText || b.textContent || '').trim()).toLowerCase();" +
                "    return t.includes('xem th');" +
                "  });" +
                "  return btns[0] || null;" +
                "}" +
                "const btn = find();" +
                "if (!btn) return null;" +
                "if (btn.disabled) return 'DISABLED';" +
                "btn.scrollIntoView({block:'center'});" +
                "btn.click();" +
                "return (btn.innerText || btn.textContent || '').trim();";

        Pattern progressPattern = Pattern.compile("\\((\\d+)\\s*/\\s*(\\d+)\\)");
        int consecutiveFails = 0;

        for (int i = 0; i < MAX_SHOW_MORE_CLICKS; i++) {
            int before = driver.findElements(GAME_ITEMS).size();
            if (before == 0) break;
            scrollToBottom();

            // Đợi button enabled — max 20s
            if (!waitForButtonEnabled(findButtonJs, 20000)) {
                LogUtils.info("Button không enable lại sau 20s, dừng");
                break;
            }

            // Throttle: 400ms giữa click để tránh server rate-limit
            pause(400);

            Object clicked;
            try {
                clicked = ((JavascriptExecutor) driver).executeScript(clickButtonJs);
            } catch (Exception e) {
                LogUtils.warn("JS click failed: " + e.getMessage());
                break;
            }
            if (clicked == null) {
                LogUtils.info("Button biến mất → đã load đủ");
                break;
            }
            if ("DISABLED".equals(clicked.toString())) {
                consecutiveFails++;
                if (consecutiveFails >= 5) break;
                pause(1000);
                continue;
            }
            clicks++;
            if (clicks == 1 || clicks % 10 == 0) {
                LogUtils.info("Xem thêm progress: " + clicked);
            }
            Matcher m = progressPattern.matcher(clicked.toString());
            if (m.find()) {
                int curr = Integer.parseInt(m.group(1));
                int total = Integer.parseInt(m.group(2));
                if (curr >= total) {
                    LogUtils.info("Đã load đủ " + total + " game");
                    break;
                }
            }
            boolean increased = waitForGameCountIncrease(before, 10000);
            if (!increased) {
                consecutiveFails++;
                LogUtils.info("Xem thêm click " + clicks + " không tăng (" + consecutiveFails + "/5)");
                if (consecutiveFails >= 5) {
                    LogUtils.info("5 click liên tiếp không tăng, dừng");
                    break;
                }
                pause(2000);
            } else {
                consecutiveFails = 0;
            }

            // Nghỉ dài sau mỗi 25 click để tránh rate-limit
            if (clicks > 0 && clicks % 25 == 0) {
                LogUtils.info("Nghỉ 3s sau " + clicks + " click để tránh rate-limit");
                pause(3000);
            }
        }
        return clicks;
    }

    /**
     * Poll JS đến khi button có status 'enabled' hoặc timeout.
     */
    private boolean waitForButtonEnabled(String findJs, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try {
                Object raw = ((JavascriptExecutor) driver).executeScript(findJs);
                if (raw == null) return false;
                String s = raw.toString();
                if (s.contains("\"status\":\"enabled\"")) return true;
                if (s.contains("\"status\":\"none\"")) return false;
            } catch (Exception ignored) {}
            pause(200);
        }
        return false;
    }

    private void waitForGameTilesOrTimeout(long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (!driver.findElements(GAME_ITEMS).isEmpty()) return;
            pause(100);
        }
    }

    private boolean waitForGameCountIncrease(int before, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            int now = driver.findElements(GAME_ITEMS).size();
            if (now > before) return true;
            pause(80);
        }
        return false;
    }

    private WebElement findVisible(By locator) {
        for (WebElement el : driver.findElements(locator)) {
            try { if (el.isDisplayed()) return el; }
            catch (Exception ignored) {}
        }
        return null;
    }

    private void scrollToBottom() {
        try {
            ((JavascriptExecutor) driver)
                    .executeScript("window.scrollTo(0, document.body.scrollHeight);");
        } catch (Exception ignored) {}
    }

    private void scrollToTop() {
        try {
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
        } catch (Exception ignored) {}
    }

    public List<GameCheckResult> scanAllGames() {
        List<GameCheckResult> results = new ArrayList<>();
        ensureValidWindow();

        int lobbyCount = driver.findElements(LOBBY_TABS).size();
        LogUtils.info("Found " + lobbyCount + " lobbies");

        for (int i = 0; i < lobbyCount; i++) {
            ensureValidWindow();
            String lobbyName = openLobbyByIndex(i);
            if (lobbyName == null) continue;

            boolean hasProvider = !driver.findElements(PROVIDER_DROPDOWN).isEmpty();
            if (hasProvider) {
                int providerCount = driver.findElements(PROVIDER_NAME).size();
                LogUtils.info("Lobby '" + lobbyName + "' has " + providerCount + " providers");
                for (int p = 0; p < providerCount; p++) {
                    ensureValidWindow();
                    String providerName = openProviderByIndex(p);
                    if (providerName == null) continue;
                    scanGamesInProvider(results, lobbyName, providerName, i, p);
                }
            } else {
                LogUtils.info("Lobby '" + lobbyName + "' has no provider");
                scanGamesInLobby(results, lobbyName, i);
            }
        }
        return results;
    }

    private void scanGamesInLobby(List<GameCheckResult> results, String lobbyName, int lobbyIndex) {
        int gameCount = driver.findElements(GAME_ITEMS).size();
        LogUtils.info("Scanning " + gameCount + " games in [" + lobbyName + " / -]");

        for (int g = 0; g < gameCount; g++) {
            if (g > 0) {
                ensureValidWindow();
                openLobbyByIndex(lobbyIndex);
            }
            checkOneGame(results, lobbyName, "-", g);
        }
    }

    private void scanGamesInProvider(List<GameCheckResult> results, String lobbyName, String providerName,
                                     int lobbyIndex, int providerIndex) {
        int gameCount = driver.findElements(GAME_ITEMS).size();
        LogUtils.info("Scanning " + gameCount + " games in [" + lobbyName + " / " + providerName + "]");

        for (int g = 0; g < gameCount; g++) {
            if (g > 0) {
                ensureValidWindow();
                openLobbyByIndex(lobbyIndex);
                openProviderByIndex(providerIndex);
            }
            checkOneGame(results, lobbyName, providerName, g);
        }
    }

    private void checkOneGame(List<GameCheckResult> results, String lobbyName, String providerName,
                              int gameIndex) {
        ensureValidWindow();
        String mainWindow;
        try {
            mainWindow = driver.getWindowHandle();
        } catch (Exception e) {
            LogUtils.warn("Không lấy được current window: " + e.getMessage());
            return;
        }

        List<WebElement> games = driver.findElements(GAME_ITEMS);
        if (gameIndex >= games.size()) {
            results.add(new GameCheckResult(lobbyName, providerName, "(index " + gameIndex + ")",
                    false, GameCheckResult.OpenMode.NONE,
                    "Không tìm được game element sau khi restore lobby"));
            return;
        }
        WebElement game = games.get(gameIndex);
        String gameName = extractGameName(game);

        Set<String> beforeHandles = new HashSet<>(driver.getWindowHandles());
        try {
            scrollIntoView(game);
            game.click();
        } catch (Exception e) {
            results.add(new GameCheckResult(lobbyName, providerName, gameName,
                    false, GameCheckResult.OpenMode.NONE,
                    "Cannot click game: " + e.getMessage()));
            return;
        }

        GameCheckResult result = detectAndVerify(lobbyName, providerName, gameName,
                beforeHandles, mainWindow);
        results.add(result);
        LogUtils.info(result.toString());
    }

    private GameCheckResult detectAndVerify(String lobbyName, String providerName, String gameName,
                                            Set<String> beforeHandles, String mainWindow) {
        // Case 1: new tab
        try {
            new WebDriverWait(driver, NEW_WINDOW_TIMEOUT)
                    .until(d -> d.getWindowHandles().size() > beforeHandles.size());
        } catch (Exception ignored) { /* no new tab */ }

        Set<String> afterHandles = new HashSet<>(driver.getWindowHandles());
        afterHandles.removeAll(beforeHandles);

        if (!afterHandles.isEmpty()) {
            String newTab = afterHandles.iterator().next();
            try {
                driver.switchTo().window(newTab);
                String url = driver.getCurrentUrl();
                String title = driver.getTitle();

                String reason;
                boolean ok;
                if (!isUrlPlayable(url)) {
                    ok = false;
                    reason = "URL không hợp lệ: " + url + " | title=" + title;
                } else {
                    String loadError = verifyDocumentHealthy(GAME_LOAD_TIMEOUT);
                    ok = loadError == null;
                    reason = ok ? "" : loadError + " | url=" + url;
                }
                return new GameCheckResult(lobbyName, providerName, gameName,
                        ok, GameCheckResult.OpenMode.NEW_TAB, reason);
            } finally {
                try { driver.close(); } catch (Exception ignored) {}
                switchToSurvivingWindow(mainWindow);
            }
        }

        // Case 2: iframe
        try {
            new WebDriverWait(driver, IFRAME_TIMEOUT)
                    .until(ExpectedConditions.presenceOfElementLocated(GAME_IFRAME));
        } catch (Exception ignored) { /* no iframe */ }

        WebElement iframe = pickLargestIframe();
        if (iframe != null) {
            String src = iframe.getAttribute("src");
            String loadError = verifyIframeHealthy(iframe, src);
            boolean ok = loadError == null;
            String reason = ok ? "" : loadError;
            return new GameCheckResult(lobbyName, providerName, gameName,
                    ok, GameCheckResult.OpenMode.IFRAME, reason);
        }

        // Case 3: không có gì xảy ra
        return new GameCheckResult(lobbyName, providerName, gameName,
                false, GameCheckResult.OpenMode.NONE,
                "Không phát hiện tab mới hoặc iframe sau khi click");
    }

    private String openLobbyByIndex(int index) {
        List<WebElement> tabs = driver.findElements(LOBBY_TABS);
        if (index >= tabs.size()) return null;
        WebElement tab = tabs.get(index);
        String name = safeText(tab);
        try {
            scrollIntoView(tab);
            tab.click();
            pause(250);
            return name;
        } catch (Exception e) {
            LogUtils.warn("Cannot click lobby '" + name + "': " + e.getMessage());
            return null;
        }
    }

    private String openProviderByIndex(int index) {
        List<WebElement> providers = driver.findElements(PROVIDER_NAME);
        if (index >= providers.size()) return null;
        WebElement provider = providers.get(index);
        String name = safeText(provider);
        try {
            scrollIntoView(provider);
            provider.click();
            pause(250);
            return name;
        } catch (Exception e) {
            LogUtils.warn("Cannot click provider '" + name + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Đợi document.readyState=complete và check body có content, không chứa
     * keyword bất thường (lỗi/bảo trì/404...). Trả null nếu OK, message nếu lỗi.
     */
    private String verifyDocumentHealthy(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).until(d ->
                    "complete".equals(((JavascriptExecutor) d).executeScript("return document.readyState"))
            );
        } catch (Exception e) {
            return "Document chưa load xong sau " + timeout.toSeconds() + "s";
        }

        String title;
        String bodyText;
        try {
            title = driver.getTitle();
            if (title == null) title = "";
            Object raw = ((JavascriptExecutor) driver).executeScript(
                    "return document.body ? document.body.innerText : '';"
            );
            bodyText = raw == null ? "" : raw.toString();
        } catch (Exception e) {
            return "Không đọc được body: " + e.getMessage();
        }

        // Không bắt buộc body có text (game streaming/canvas thường body rỗng).
        // Chỉ scan keyword bất thường nếu có text.
        String combined = (title + " " + bodyText).toLowerCase();
        for (String kw : ERROR_KEYWORDS) {
            if (combined.contains(kw)) {
                return "Phát hiện từ khoá bất thường: '" + kw + "' | title=" + title;
            }
        }
        return null;
    }

    /**
     * Chọn iframe lớn nhất trên trang (game iframe luôn to nhất, các iframe rác
     * như chat widget/analytics thường 0x0 hoặc rất nhỏ).
     */
    private WebElement pickLargestIframe() {
        List<WebElement> iframes = driver.findElements(GAME_IFRAME);
        if (iframes.isEmpty()) return null;
        WebElement best = null;
        long bestArea = 0;
        for (WebElement f : iframes) {
            try {
                Number w = (Number) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].clientWidth;", f);
                Number h = (Number) ((JavascriptExecutor) driver)
                        .executeScript("return arguments[0].clientHeight;", f);
                long area = (w == null ? 0 : w.longValue()) * (h == null ? 0 : h.longValue());
                if (area > bestArea) {
                    bestArea = area;
                    best = f;
                }
            } catch (Exception ignored) {}
        }
        return best;
    }

    /**
     * Verify iframe game đã load: src hợp lệ, kích thước OK, và (nếu same-origin)
     * nội dung iframe healthy. Cross-origin iframe được coi là OK nếu element load.
     */
    private String verifyIframeHealthy(WebElement iframe, String src) {
        if (!isUrlPlayable(src)) {
            return "Iframe có src không hợp lệ: " + src;
        }
        try {
            Long w = ((Number) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].clientWidth;", iframe)).longValue();
            Long h = ((Number) ((JavascriptExecutor) driver)
                    .executeScript("return arguments[0].clientHeight;", iframe)).longValue();
            if (w == null || w < MIN_IFRAME_DIMENSION || h == null || h < MIN_IFRAME_DIMENSION) {
                return "Iframe quá nhỏ (" + w + "x" + h + ") — có thể không render";
            }
        } catch (Exception e) {
            return "Không đọc được kích thước iframe: " + e.getMessage();
        }

        // Cố switch vào iframe để check nội dung — fail = cross-origin (bình thường)
        try {
            driver.switchTo().frame(iframe);
            String err = verifyDocumentHealthy(GAME_LOAD_TIMEOUT);
            driver.switchTo().defaultContent();
            return err;
        } catch (Exception e) {
            try { driver.switchTo().defaultContent(); } catch (Exception ignored) {}
            // Cross-origin: không peek được nội dung — chấp nhận nếu src hợp lệ + dimension OK
            return null;
        }
    }

    private boolean isUrlPlayable(String url) {
        if (url == null || url.isBlank()) return false;
        String u = url.toLowerCase();
        if (u.equals("about:blank") || u.equals("data:,")) return false;
        // Chỉ reject các pattern lỗi cụ thể (path/hostname), tránh match "404" trong session ID ngẫu nhiên
        String[] badPatterns = {
                "/error", "/404", "/maintenance", "/under-maintenance", "/page-not-found",
                "?error=", "&error=", "error.html", "404.html", "maintenance.html",
                "//error.", "//maintenance."
        };
        for (String pat : badPatterns) {
            if (u.contains(pat)) return false;
        }
        return true;
    }

    private String extractGameName(WebElement game) {
        By[] candidates = {
                By.xpath(".//p[contains(@class,'uppercase')]"),
                By.xpath(".//p"),
                By.xpath(".//div[contains(@class,'name') or contains(@class,'title')]"),
                By.xpath(".//span[contains(@class,'name') or contains(@class,'title')]"),
                By.xpath(".//img")
        };
        for (By by : candidates) {
            try {
                WebElement e = game.findElement(by);
                String text = safeText(e);
                if (!text.isBlank()) return text;
                String alt = e.getAttribute("alt");
                if (alt != null && !alt.isBlank()) return alt.trim();
                String title = e.getAttribute("title");
                if (title != null && !title.isBlank()) return title.trim();
            } catch (Exception ignored) {}
        }
        // Fallback: lấy text toàn tile, dòng đầu non-empty
        String raw = safeText(game);
        if (!raw.isBlank()) {
            for (String line : raw.split("\\R")) {
                if (!line.isBlank()) return line.trim();
            }
        }
        return "(unnamed)";
    }

    private String safeText(WebElement el) {
        try {
            String t = el.getText();
            return t == null ? "" : t.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void scrollIntoView(WebElement el) {
        ((JavascriptExecutor) driver)
                .executeScript("arguments[0].scrollIntoView({block:'center'});", el);
    }

    private void pause(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    /**
     * Đảm bảo driver đang ở 1 window còn sống. Nếu current window đã đóng
     * (NoSuchWindow), tự switch sang handle bất kỳ còn lại.
     */
    private void ensureValidWindow() {
        try {
            driver.getCurrentUrl();
        } catch (NoSuchWindowException e) {
            Set<String> handles;
            try { handles = driver.getWindowHandles(); }
            catch (Exception ex) { return; }
            if (!handles.isEmpty()) {
                try { driver.switchTo().window(handles.iterator().next()); }
                catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    /**
     * Sau khi đóng tab game, switch về preferred handle nếu còn sống,
     * nếu không thì switch về handle bất kỳ. Trả về handle đã switch tới.
     */
    private String switchToSurvivingWindow(String preferred) {
        Set<String> handles;
        try { handles = driver.getWindowHandles(); }
        catch (Exception e) { return null; }
        if (handles.isEmpty()) return null;
        String target = handles.contains(preferred) ? preferred : handles.iterator().next();
        try {
            driver.switchTo().window(target);
            return target;
        } catch (Exception e) {
            LogUtils.warn("Không switch được sang window " + target + ": " + e.getMessage());
            return null;
        }
    }
}