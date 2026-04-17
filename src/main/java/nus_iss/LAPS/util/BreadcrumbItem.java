package nus_iss.LAPS.util;

/**
 * Represents a single breadcrumb item.
 * Used to build breadcrumb navigation trails in templates.
 *
 * Example:
 *   new BreadcrumbItem("Home", "/")
 *   new BreadcrumbItem("Leave", "/leave")
 *   new BreadcrumbItem("Apply for Leave", null)  // null = current page (not clickable)
 *
 * Author: Htet Nandar (Grace)
 */
public class BreadcrumbItem {
    private String label;
    private String url;

    /**
     * @param label Display text for the breadcrumb
     * @param url   URL path (null for current/active item)
     */
    public BreadcrumbItem(String label, String url) {
        this.label = label;
        this.url = url;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "BreadcrumbItem{" +
                "label='" + label + '\'' +
                ", url='" + url + '\'' +
                '}';
    }
}

