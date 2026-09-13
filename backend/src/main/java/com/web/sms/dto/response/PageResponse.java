package com.web.sms.dto.response;

import org.springframework.data.domain.Page;
import java.util.List;

public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public PageResponse(Page<T> pageResult) {
        this.content = pageResult.getContent();
        this.page = pageResult.getNumber();
        this.size = pageResult.getSize();
        this.totalElements = pageResult.getTotalElements();
        this.totalPages = pageResult.getTotalPages();
        this.last = pageResult.isLast();
    }
    
    public List<T> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public boolean isLast() { return last; }
}
