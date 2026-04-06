/*
 * Copyright (C) 2021 taufik
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.kalsym.ekedai.model;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class CustomPageable {
    public Object[] content;
    public Pageable pageable;
    public int totalPages;
    public long totalElements;    
    public boolean last;
    public int size;
    public int number;
    public Sort sort;
    public long numberOfElements;
    public boolean first;
    public boolean empty;
}
