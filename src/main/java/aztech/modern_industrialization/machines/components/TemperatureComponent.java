/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package aztech.modern_industrialization.machines.components;

import aztech.modern_industrialization.machines.IComponent;
import net.minecraft.nbt.CompoundTag;

public class TemperatureComponent implements IComponent {

    private double temperature;
    public final double temperatureMax;

    public TemperatureComponent(double temperatureMax) {
        this.temperatureMax = temperatureMax;
    }

    public void setTemperature(double temp) {
        this.temperature = temp;
        this.temperature = Math.min(Math.max(temperature, 0), temperatureMax);
    }

    public void increaseTemperature(double temp) {
        setTemperature(getTemperature() + temp);
    }

    public void decreaseTemperature(double temp) {
        setTemperature(getTemperature() - temp);
    }

    public double getTemperature() {
        return temperature;
    }

    @Override
    public void writeNbt(CompoundTag tag) {
        tag.putDouble("temperature", temperature);

    }

    @Override
    public void readNbt(CompoundTag tag) {
        setTemperature(tag.getDouble("temperature"));
    }
}
