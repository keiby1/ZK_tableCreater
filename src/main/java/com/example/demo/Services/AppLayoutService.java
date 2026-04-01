package com.example.demo.Services;

import org.springframework.stereotype.Service;

/**
 * Общий хедер/меню и блок расшифровки индикаторов для всех HTML-страниц приложения.
 */
@Service
public class AppLayoutService {

    /**
     * Возвращает HTML фрагмент: навбар с кнопками быстрых действий и блок «Расшифровка индикаторов».
     * Вставляется в начало &lt;body&gt; на каждой странице.
     */
    public String buildAppHeader() {
        return ""
                + "    <header class=\"app-header\">\n"
                + "      <nav class=\"app-nav\">\n"
                + "        <a id=\"app-nav-json\" href=\"/getJson\" class=\"app-nav-link\" target=\"_blank\">Сохранить JSON</a>\n"
                + "        <a id=\"app-nav-html\" href=\"/getHtml\" class=\"app-nav-link\">Сохранить HTML в файл</a>\n"
                + "        <a href=\"/compare\" class=\"app-nav-link\">Сравнить</a>\n"
                + "      </nav>\n"
                + "      <div class=\"app-legend\">\n"
                + "        <details>\n"
                + "          <summary>Расшифровка индикаторов</summary>\n"
                + "          <ul>\n"
                + "            <li><span class=\"swatch\" style=\"background:#c8e6c9\"></span> CPU/память: оптимальная утилизация (60–79%)</li>\n"
                + "            <li><span class=\"swatch\" style=\"background:#fff9c4\"></span> CPU/память: низкая утилизация (20–59%)</li>\n"
                + "            <li><span class=\"swatch\" style=\"background:#ffcdd2\"></span> CPU/память: очень низкая (0–19%)</li>\n"
                + "            <li><span class=\"swatch\" style=\"background:#ef9a9a\"></span> CPU/память: критическая утилизация (≥80%)</li>\n"
                + "            <li><span class=\"swatch\" style=\"background:#78909c\"></span> Превышен лимит деплоймента по столбцу (CPU/RAM)</li>\n"
                + "            <li><span class=\"swatch\" style=\"background:#c8e6c9\"></span> Время старта: до 1 мин</li>\n"
                + "            <li><span class=\"swatch\" style=\"background:#fff9c4\"></span> Время старта: 1–1.5 мин</li>\n"
                + "            <li><span class=\"swatch\" style=\"background:#ffe0b2\"></span> Время старта: 1.5–2 мин</li>\n"
                + "            <li><span class=\"swatch\" style=\"background:#ffcdd2\"></span> Время старта: более 2 мин</li>\n"
                + "            <li>Троттлинг: ≤1% — зелёный; &gt;1% и ≤3% — жёлтый; &gt;3% и ≤5% — оранжевый; &gt;5% — красный</li>\n"
                + "          </ul>\n"
                + "        </details>\n"
                + "      </div>\n"
                + "    </header>\n"
                + "    <style>\n"
                + "      .app-header { margin: -20px -20px 20px -20px; padding: 12px 20px; background: #fff; border-bottom: 1px solid #e0e0e0; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }\n"
                + "      .app-nav { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; margin-bottom: 12px; }\n"
                + "      .app-nav-link { display: inline-block; padding: 8px 16px; background: #4CAF50; color: #fff; text-decoration: none; border-radius: 6px; font-size: 0.95em; }\n"
                + "      .app-nav-link:hover { background: #43A047; }\n"
                + "      .app-nav-link:first-of-type { margin-right: 0; }\n"
                + "      .app-legend { margin-top: 8px; padding: 10px 0 0 0; border-top: 1px solid #eee; }\n"
                + "      .app-legend details summary { cursor: pointer; font-weight: bold; color: #424242; list-style: none; }\n"
                + "      .app-legend details summary::-webkit-details-marker { display: none; }\n"
                + "      .app-legend details summary::before { content: '▶ '; font-size: 0.75em; }\n"
                + "      .app-legend details[open] summary::before { content: '▼ '; }\n"
                + "      .app-legend ul { margin: 6px 0 0 0; padding-left: 20px; font-size: 0.9em; }\n"
                + "      .app-legend li { margin: 2px 0; }\n"
                + "      .app-legend .swatch { display: inline-block; width: 14px; height: 14px; margin-right: 6px; vertical-align: middle; border: 1px solid #bdbdbd; border-radius: 2px; }\n"
                + "    </style>\n"
                + "    <script>\n"
                + "      (function() {\n"
                + "        var q = window.location.search || '';\n"
                + "        var jsonLink = document.getElementById('app-nav-json');\n"
                + "        var htmlLink = document.getElementById('app-nav-html');\n"
                + "        if (jsonLink) jsonLink.setAttribute('href', '/getJson' + q);\n"
                + "        if (htmlLink) htmlLink.setAttribute('href', '/getHtml' + q);\n"
                + "      })();\n"
                + "    </script>\n";
    }
}
