package de.fiz.oai.backend.models;

import java.util.List;

public class SearchResult<E> {
  int total;
  int offset;
  int size;
  
  List<E> data;

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public List<E> getData() {
    return data;
  }

  public void setData(List<E> data) {
    this.data = data;
  }

  
}
