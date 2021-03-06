/*
 * Copyright 2017 Long Term Software LLC
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ltsllc.miranda.file;

import com.ltsllc.miranda.reader.Reader;
import com.ltsllc.miranda.writer.Writer;

import java.io.IOException;

/**
 * Created by Clark on 2/26/2017.
 */
abstract public class PerishableFile<E extends Perishable & Updateable<E> & Matchable<E>> extends SingleFile<E> {
    public PerishableFile (String filename, Reader reader, Writer writer) throws IOException {
        super(filename, reader, writer);
    }
}
