/*
 * Copyright 2024-2026 Jared Ottley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.ottleys.xpository.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for collection behavior and constraints.
 *
 * <p>Defines configurable limits for LIST and TREE collection depth, and controls soft delete
 * behavior including retention period and automatic cleanup settings.
 */
@Configuration
@ConfigurationProperties(prefix = "xpository.collections")
public class CollectionProperties {

  private ListConfig list = new ListConfig();
  private TreeConfig tree = new TreeConfig();
  private SoftDeleteConfig softDelete = new SoftDeleteConfig();

  public ListConfig getList() {
    return list;
  }

  public void setList(ListConfig list) {
    this.list = list;
  }

  public TreeConfig getTree() {
    return tree;
  }

  public void setTree(TreeConfig tree) {
    this.tree = tree;
  }

  public SoftDeleteConfig getSoftDelete() {
    return softDelete;
  }

  public void setSoftDelete(SoftDeleteConfig softDelete) {
    this.softDelete = softDelete;
  }

  public static class ListConfig {
    /** Maximum nesting depth for LIST collections. Default: 10 */
    private int maxDepth = 10;

    public int getMaxDepth() {
      return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
      this.maxDepth = maxDepth;
    }
  }

  public static class TreeConfig {
    /** Maximum tree depth for TREE collections. Default: 20 */
    private int maxDepth = 20;

    public int getMaxDepth() {
      return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
      this.maxDepth = maxDepth;
    }
  }

  public static class SoftDeleteConfig {
    /** How many days to retain soft-deleted collections before permanent removal. Default: 30 */
    private int retentionDays = 30;

    /** Enable automatic cleanup of expired soft-deleted collections. Default: true */
    private boolean autoCleanupEnabled = true;

    public int getRetentionDays() {
      return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
      this.retentionDays = retentionDays;
    }

    public boolean isAutoCleanupEnabled() {
      return autoCleanupEnabled;
    }

    public void setAutoCleanupEnabled(boolean autoCleanupEnabled) {
      this.autoCleanupEnabled = autoCleanupEnabled;
    }
  }
}
