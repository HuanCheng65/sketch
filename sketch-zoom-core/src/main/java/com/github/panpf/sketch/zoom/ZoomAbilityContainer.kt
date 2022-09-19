/*
 * Copyright (C) 2022 panpf <panpfpanpf@outlook.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.panpf.sketch.zoom

import android.graphics.Matrix
import android.widget.ImageView.ScaleType
import com.github.panpf.sketch.zoom.util.InBitmapHelper
import com.github.panpf.sketch.zoom.util.Logger

/**
 * Provides access services for ViewAbility registration, uninstallation, and event callbacks and properties
 */
interface ZoomAbilityContainer {

    var logger: Logger

    var inBitmapHelper: InBitmapHelper

    /**
     * Call the parent class's setScaleType() method
     */
    fun superSetScaleType(scaleType: ScaleType)

    /**
     * Call the parent class's getScaleType() method
     */
    fun superGetScaleType(): ScaleType

    /**
     * Call the parent class's setImageMatrix() method
     */
    fun superSetImageMatrix(matrix: Matrix?)

    /**
     * Call the parent class's getImageMatrix() method
     */
    fun superGetImageMatrix(): Matrix?
}