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
package net.ottleys.xpository.domain;

/**
 * Defines the structural organization type for a collection.
 *
 * <p>LIST supports nested structural elements with order and depth tracking. TREE supports
 * hierarchical organization with materialized paths. GRAPH supports many-to-many relationships
 * (Phase 2). STACK supports LIFO element ordering (Phase 2).
 */
public enum CollectionType {
  LIST,
  TREE,
  GRAPH,
  STACK
}
