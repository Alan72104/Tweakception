package a7.tweakception.utils;

public class WindowClickContants
{
    public static class LeftRight
    {
        public static final int MODE = 0;
        public static final int BTN_LEFT = 0;
        public static final int BTN_RIGHT = 1;
    }
    
    public static class ShiftLeftRight
    {
        public static final int MODE = 1;
        public static final int BTN_SHIFT_LEFT = 0;
        public static final int BTN_SHIFT_RIGHT = 1;
    }
    
    public static class Number
    {
        public static final int MODE = 2;
        public static final int BTN_1 = 0;
        public static final int BTN_2 = 1;
        public static final int BTN_3 = 2;
        public static final int BTN_4 = 3;
        public static final int BTN_5 = 4;
        public static final int BTN_6 = 5;
        public static final int BTN_7 = 6;
        public static final int BTN_8 = 7;
        public static final int BTN_9 = 8;
    }
    
    public static class Middle
    {
        public static final int MODE = 3;
        public static final int BTN = 2;
    }
    
    public static class Drop
    {
        public static final int MODE = 4;
        public static final int BTN_DROP = 0;
        public static final int BTN_CTRL_DROP = 1;
        /**
         * Slot number is -999
         */
        public static final int BTN_LEFT_OUTSIDE_INV = 0;
        /**
         * Slot number is -999
         */
        public static final int BTN_RIGHT_OUTSIDE_INV = 1;
    }
    
    public static class Drag
    {
        public static final int MODE = 5;
        /**
         * Slot number is -999
         */
        public static final int START_LEFT = 0;
        /**
         * Slot number is -999
         */
        public static final int START_RIGHT = 4;
        public static final int ADD_LEFT = 1;
        public static final int ADD_RIGHT = 5;
        /**
         * Slot number is -999
         */
        public static final int END_LEFT = 2;
        /**
         * Slot number is -999
         */
        public static final int END_RIGHT = 6;
    }
    
    public static class DoubleClick
    {
        public static final int MODE = 6;
        public static final int BTN = 0;
    }
}
