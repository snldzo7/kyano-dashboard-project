``` c

typedef struct Clay_String {
    // Set this boolean to true if the char* data underlying this string will live for the entire lifetime of the program.
    // This will automatically be set for strings created with CLAY_STRING, as the macro requires a string literal.
    bool isStaticallyAllocated;
    int32_t length;
    // The underlying character memory. Note: this will not be copied and will not extend the lifetime of the underlying memory.
    const char *chars;
} Clay_String;

typedef struct Clay_StringSlice {
    int32_t length;
    const char *chars;
    const char *baseChars; // The source string / char* that this slice was derived from
} Clay_StringSlice;

typedef struct Clay_Arena {
    uintptr_t nextAllocation;
    size_t capacity;
    char *memory;
} Clay_Arena;

typedef struct Clay_Dimensions {
    float width, height;
} Clay_Dimensions;

typedef struct Clay_Vector2 {
    float x, y;
} Clay_Vector2;

typedef struct Clay_Color {
    uint8_t r, g, b, a;
} Clay_Color;

typedef struct Clay_HoverState {
    Clay_ElementId elementId;
    bool wasHoveredLastFrame;
} Clay_HoverState;

typedef struct Clay_Axis2 {
    Clay_Dimensions x;
    Clay_Dimensions y;
} Clay_Axis2;

typedef struct Clay_ColorPalette {
    Clay_Color backgroundColor;
    Clay_Color panelColor;
    Clay_Color borderColor;
    Clay_Color borderColorBright;
    Clay_Color iconColor;
    Clay_Color textColor;
    Clay_Color buttonColor;
    Clay_Color buttonColorBright;
    Clay_Color accentColor;
    Clay_Color scrollbarThumbColor;
    Clay_Color scrollbarTrackColor;
    Clay_Color inputBorderColor;
    Clay_Color inputBorderErrorColor;
    Clay_Color labelTextColor;
    Clay_Color textSecondary;
    Clay_Color successColor;
    Clay_Color overlayColor;
} Clay_ColorPalette;

typedef struct Clay_StringArrayElement {
    Clay_String value;
} Clay_StringArrayElement;

typedef struct Clay_StringArray {
    int32_t capacity;
    int32_t length;
    Clay_StringArrayElement *internalArray;
} Clay_StringArray;

typedef struct Clay_Int32ArrayElement {
    int32_t value;
} Clay_Int32ArrayElement;

typedef struct Clay_Int32Array {
    int32_t capacity;
    int32_t length;
    Clay_Int32ArrayElement *internalArray;
} Clay_Int32Array;

typedef struct Clay_FloatArrayElement {
    float value;
} Clay_FloatArrayElement;

typedef struct Clay_FloatArray {
    int32_t capacity;
    int32_t length;
    Clay_FloatArrayElement *internalArray;
} Clay_FloatArray;

typedef struct Clay_DimensionsArrayElement {
    Clay_Dimensions value;
} Clay_DimensionsArrayElement;

typedef struct Clay_DimensionsArray {
    int32_t capacity;
    int32_t length;
    Clay_DimensionsArrayElement *internalArray;
} Clay_DimensionsArray;

typedef struct Clay_Vector2ArrayElement {
    Clay_Vector2 value;
} Clay_Vector2ArrayElement;

typedef struct Clay_Vector2Array {
    int32_t capacity;
    int32_t length;
    Clay_Vector2ArrayElement *internalArray;
} Clay_Vector2Array;

typedef struct Clay_ColorArrayElement {
    Clay_Color value;
} Clay_ColorArrayElement;

typedef struct Clay_ColorArray {
    int32_t capacity;
    int32_t length;
    Clay_ColorArrayElement *internalArray;
} Clay_ColorArray;

typedef struct Clay_ColorPaletteArrayElement {
    Clay_ColorPalette value;
} Clay_ColorPaletteArrayElement;

typedef struct Clay_ColorPaletteArray {
    int32_t capacity;
    int32_t length;
    Clay_ColorPaletteArrayElement *internalArray;
} Clay_ColorPaletteArray;

typedef struct Clay_ElementId {
    uint32_t id; // The resulting hash generated from the other fields.
    uint32_t offset; // A numerical offset applied after computing the hash from stringId.
    uint32_t baseId; // A base hash value to start from, for example the parent element ID is used when calculating CLAY_ID_LOCAL().
    Clay_String stringId; // The string id to hash.
} Clay_ElementId;

typedef struct
{
    int32_t capacity;
    int32_t length;
    Clay_ElementId *internalArray;
} Clay_ElementIdArray;

typedef struct Clay_CornerRadius {
    float topLeft;
    float topRight;
    float bottomLeft;
    float bottomRight;
} Clay_CornerRadius;

typedef struct Clay_SizingAxis {
    Clay_Sizing_Kind kind;
    struct {
        Clay_SizingAxis_ManualSize_Kind kind;
        float value;
    } manualSize;
    struct {
        float basis;
        float min;
        float max;
    } size;
    struct {
        float min;
        float max;
    } minMax;
} Clay_SizingAxis;

typedef struct Clay_SizingAxisArrayElement {
    Clay_SizingAxis value;
} Clay_SizingAxisArrayElement;

typedef struct Clay_SizingAxisArray {
    int32_t capacity;
    int32_t length;
    Clay_SizingAxisArrayElement *internalArray;
} Clay_SizingAxisArray;

typedef struct Clay_Sizing {
    Clay_SizingAxis width;
    Clay_SizingAxis height;
    bool isWidthExpandable;
    bool isHeightExpandable;
} Clay_Sizing;

typedef struct Clay_Spacing {
    float x;
    float y;
} Clay_Spacing;

typedef struct Clay_LayoutConfig {
    Clay_LayoutDirection layoutDirection;
    Clay_ItemAlignment itemAlignment;
    Clay_LayoutAxis primaryAxis;
    Clay_LayoutAxis crossAxis;
    Clay_Sizing sizing;
    Clay_Spacing padding;
    Clay_Spacing margin;
    Clay_Spacing childSpacing;
    Clay_CornerRadius cornerRadius;
    Clay_Bool clipChildren;
    Clay_Bool hasBackgroundColor;
    Clay_Color backgroundColor;
    Clay_Bool hasBorder;
    float borderThickness;
    Clay_Color borderColor;
    Clay_LayoutOverflow layoutOverflow;
    Clay_Bool isDisabled;
    Clay_Bool isDisabledSet;
    Clay_Bool isHovered;
    Clay_Bool isHoveredSet;
    Clay_Bool isPressed;
    Clay_Bool isPressedSet;
    Clay_CursorAppearance cursorAppearance;
    Clay_Bool hasClickCallback;
    Clay_LayoutClickCallback clickCallback;
    Clay_Bool hasLayoutTag;
    uint32_t layoutTag;
} Clay_LayoutConfig;

typedef struct Clay_LayoutConfigArrayElement {
    Clay_LayoutConfig value;
} Clay_LayoutConfigArrayElement;

typedef struct Clay_LayoutConfigArray {
    int32_t capacity;
    int32_t length;
    Clay_LayoutConfigArrayElement *internalArray;
} Clay_LayoutConfigArray;

typedef struct Clay_TextElementConfig {
    Clay_ColorPalette *colorPalette;
    Clay_Color textColor;
    Clay_Bool hasTextColor;
    Clay_TextElementConfig_Overflow overflow;
    Clay_TextElementConfig_WrapMode wrapMode;
    Clay_TextElementConfig_HorizontalAlign horizontalAlign;
    Clay_TextElementConfig_VerticalAlign verticalAlign;
} Clay_TextElementConfig;

typedef struct Clay_TextElementConfigArrayElement {
    Clay_TextElementConfig value;
} Clay_TextElementConfigArrayElement;

typedef struct Clay_TextElementConfigArray {
    int32_t capacity;
    int32_t length;
    Clay_TextElementConfigArrayElement *internalArray;
} Clay_TextElementConfigArray;

typedef struct Clay_ScrollConfig {
    Clay_ScrollConfig_Kind kind;
    Clay_Dimensions scrollPosition;
    Clay_Dimensions contentSize;
    Clay_Dimensions viewportSize;
    Clay_Bool hasFunnels;
    float scrollSpeed;
    float mouseWheelMultiplier;
} Clay_ScrollConfig;

typedef struct Clay_ScrollConfigArrayElement {
    Clay_ScrollConfig value;
} Clay_ScrollConfigArrayElement;

typedef struct Clay_ScrollConfigArray {
    int32_t capacity;
    int32_t length;
    Clay_ScrollConfigArrayElement *internalArray;
} Clay_ScrollConfigArray;

typedef struct Clay_InputElementConfig {
    Clay_ColorPalette *colorPalette;
    Clay_Color backgroundColor;
    Clay_Bool hasBackgroundColor;
    Clay_Color borderColor;
    Clay_Bool hasBorderColor;
    bool isPassword;
    uint32_t maxLength;
    Clay_String placeholder;
} Clay_InputElementConfig;

typedef struct Clay_InputElementConfigArrayElement {
    Clay_InputElementConfig value;
} Clay_InputElementConfigArrayElement;

typedef struct Clay_InputElementConfigArray {
    int32_t capacity;
    int32_t length;
    Clay_InputElementConfigArrayElement *internalArray;
} Clay_InputElementConfigArray;

typedef struct Clay_Float2 {
    float x, y;
} Clay_Float2;

typedef struct Clay_BoundingBox {
    Clay_Float2 position;
    Clay_Float2 size;
} Clay_BoundingBox;

typedef struct Clay_ScrollbarConfig {
    Clay_ColorPalette *colorPalette;
    float thickness;
    float cornerRadius;
    Clay_Bool hasBackground;
    Clay_Color backgroundColor;
    Clay_Bool showTrack;
    Clay_Color trackColor;
    Clay_Bool showThumb;
    Clay_Color thumbColor;
} Clay_ScrollbarConfig;

typedef struct Clay_ScrollbarConfigArrayElement {
    Clay_ScrollbarConfig value;
} Clay_ScrollbarConfigArrayElement;

typedef struct Clay_ScrollbarConfigArray {
    int32_t capacity;
    int32_t length;
    Clay_ScrollbarConfigArrayElement *internalArray;
} Clay_ScrollbarConfigArray;

typedef struct Clay_TooltipConfig {
    Clay_ColorPalette *colorPalette;
    Clay_Dimensions maxSize;
    Clay_Bool hasMaxSize;
    float margin;
    float padding;
} Clay_TooltipConfig;

typedef struct Clay_TooltipConfigArrayElement {
    Clay_TooltipConfig value;
} Clay_TooltipConfigArrayElement;

typedef struct Clay_TooltipConfigArray {
    int32_t capacity;
    int32_t length;
    Clay_TooltipConfigArrayElement *internalArray;
} Clay_TooltipConfigArray;

typedef struct {
    Clay_ElementId id;
    Clay_BoundingBox box;
} Clay_HoveredTooltip;

typedef struct Clay_TooltipArrayElement {
    Clay_HoveredTooltip value;
} Clay_TooltipArrayElement;

typedef struct Clay_TooltipArray {
    int32_t capacity;
    int32_t length;
    Clay_TooltipArrayElement *internalArray;
} Clay_TooltipArray;

typedef struct Clay_HoverConstraintConfig {
    Clay_Float2 offset;
    Clay_Dimensions maxSize;
    float padding;
} Clay_HoverConstraintConfig;

typedef struct Clay_HoverConstraintConfigArrayElement {
    Clay_HoverConstraintConfig value;
} Clay_HoverConstraintConfigArrayElement;

typedef struct Clay_HoverConstraintConfigArray {
    int32_t capacity;
    int32_t length;
    Clay_HoverConstraintConfigArrayElement *internalArray;
} Clay_HoverConstraintConfigArray;

typedef struct Clay_RenderCommand_ColoredQuad {
    Clay_BoundingBox bounds;
    Clay_Color color;
    float cornerRadiusTopLeft;
    float cornerRadiusTopRight;
    float cornerRadiusBottomRight;
    float cornerRadiusBottomLeft;
} Clay_RenderCommand_ColoredQuad;

typedef struct Clay_RenderCommand_ColoredQuadArrayElement {
    Clay_RenderCommand_ColoredQuad value;
} Clay_RenderCommand_ColoredQuadArrayElement;

typedef struct Clay_RenderCommand_ColoredQuadArray {
    int32_t capacity;
    int32_t length;
    Clay_RenderCommand_ColoredQuadArrayElement *internalArray;
} Clay_RenderCommand_ColoredQuadArray;

typedef struct Clay_RenderCommand_Text {
    Clay_BoundingBox bounds;
    Clay_String text;
    Clay_Color color;
} Clay_RenderCommand_Text;

typedef struct Clay_RenderCommand_TextArrayElement {
    Clay_RenderCommand_Text value;
} Clay_RenderCommand_TextArrayElement;

typedef struct Clay_RenderCommand_TextArray {
    int32_t capacity;
    int32_t length;
    Clay_RenderCommand_TextArrayElement *internalArray;
} Clay_RenderCommand_TextArray;

typedef struct Clay_RenderCommand_Icon {
    Clay_BoundingBox bounds;
    Clay_String iconId;
    Clay_Color color;
} Clay_RenderCommand_Icon;

typedef struct Clay_RenderCommand_IconArrayElement {
    Clay_RenderCommand_Icon value;
} Clay_RenderCommand_IconArrayElement;

typedef struct Clay_RenderCommand_IconArray {
    int32_t capacity;
    int32_t length;
    Clay_RenderCommand_IconArrayElement *internalArray;
} Clay_RenderCommand_IconArray;

typedef struct Clay_RenderCommand_Input {
    Clay_BoundingBox bounds;
    Clay_String text;
    Clay_String placeholder;
    bool isPassword;
    Clay_String fontId;
    float fontSize;
    Clay_String iconId;
    Clay_ColorPalette *colorPalette;
} Clay_RenderCommand_Input;

typedef struct Clay_RenderCommand_InputArrayElement {
    Clay_RenderCommand_Input value;
} Clay_RenderCommand_InputArrayElement;

typedef struct Clay_RenderCommand_InputArray {
    int32_t capacity;
    int32_t length;
    Clay_RenderCommand_InputArrayElement *internalArray;
} Clay_RenderCommand_InputArray;

typedef struct Clay_RenderCommand_Scrollbar {
    Clay_BoundingBox trackBounds;
    Clay_BoundingBox thumbBounds;
    Clay_Bool isHorizontal;
    Clay_ColorPalette *colorPalette;
    float thickness;
} Clay_RenderCommand_Scrollbar;

typedef struct Clay_RenderCommand_ScrollbarArrayElement {
    Clay_RenderCommand_Scrollbar value;
} Clay_RenderCommand_ScrollbarArrayElement;

typedef struct Clay_RenderCommand_ScrollbarArray {
    int32_t capacity;
    int32_t length;
    Clay_RenderCommand_ScrollbarArrayElement *internalArray;
} Clay_RenderCommand_ScrollbarArray;

typedef struct Clay_RenderCommand_Tooltip {
    Clay_BoundingBox bounds;
    Clay_String text;
    Clay_ColorPalette *colorPalette;
    float padding;
} Clay_RenderCommand_Tooltip;

typedef struct Clay_RenderCommand_TooltipArrayElement {
    Clay_RenderCommand_Tooltip value;
} Clay_RenderCommand_TooltipArrayElement;

typedef struct Clay_RenderCommand_TooltipArray {
    int32_t capacity;
    int32_t length;
    Clay_RenderCommand_TooltipArrayElement *internalArray;
} Clay_RenderCommand_TooltipArray;

typedef struct Clay_RenderCommand_Clip {
    Clay_BoundingBox bounds;
    Clay_Bool enable;
} Clay_RenderCommand_Clip;

typedef struct Clay_RenderCommand_ClipArrayElement {
    Clay_RenderCommand_Clip value;
} Clay_RenderCommand_ClipArrayElement;

typedef struct Clay_RenderCommand_ClipArray {
    int32_t capacity;
    int32_t length;
    Clay_RenderCommand_ClipArrayElement *internalArray;
} Clay_RenderCommand_ClipArray;

typedef struct Clay_RenderCommandArrayElement {
    Clay_RenderCommand value;
} Clay_RenderCommandArrayElement;

typedef struct Clay_RenderCommandArray {
    int32_t capacity;
    int32_t length;
    Clay_RenderCommandArrayElement *internalArray;
} Clay_RenderCommandArray;

typedef struct Clay_RenderCommandGroup {
    Clay_BoundingBox bounds;
    Clay_RenderCommandArray commands;
} Clay_RenderCommandGroup;

typedef struct Clay_RenderCommandGroupArrayElement {
    Clay_RenderCommandGroup value;
} Clay_RenderCommandGroupArrayElement;

typedef struct Clay_RenderCommandGroupArray {
    int32_t capacity;
    int32_t length;
    Clay_RenderCommandGroupArrayElement *internalArray;
} Clay_RenderCommandGroupArray;

typedef struct Clay_LayoutElement {
    Clay_ElementId id;
    Clay_LayoutConfig *layoutConfig;
    Clay_BoundingBox boundingBox;
    Clay_Dimensions minSize;
    Clay_Dimensions maxSize;
    Clay_Dimensions preferredSize;
    Clay_Dimensions contentSize;
    Clay_ScrollConfig *scrollConfig;
    Clay_TextElementConfig *textConfig;
    Clay_InputElementConfig *inputConfig;
    Clay_ScrollbarConfig *scrollbarConfig;
    Clay_TooltipConfig *tooltipConfig;
    Clay_HoverConstraintConfig *hoverConstraintConfig;
    Clay_ElementIdArray children;
    Clay_ElementId parent;
    Clay_LayoutElement_FlexLayoutElement flexLayoutElement;
    Clay_LayoutElement_FloatingLayoutElement floatingLayoutElement;
    Clay_LayoutElement_ScrollLayoutElement scrollLayoutElement;
} Clay_LayoutElement;

typedef struct Clay_LayoutElementArrayElement {
    Clay_LayoutElement value;
} Clay_LayoutElementArrayElement;

typedef struct Clay_LayoutElementArray {
    int32_t capacity;
    int32_t length;
    Clay_LayoutElementArrayElement *internalArray;
} Clay_LayoutElementArray;

typedef struct Clay_LayoutState {
    Clay_LayoutElementArray elements;
    Clay_ElementIdArray elementStack;
    Clay_ElementIdArray clippingStack;
    Clay_RenderCommandArray renderCommands;
    Clay_RenderCommandGroupArray renderCommandGroups;
    Clay_HoverState hoverState;
    Clay_BoundingBox scissorRect;
    Clay_BooleanWarnings warnings;
    Clay_ScrollConfigArray scrollConfigs;
    Clay_TextElementConfigArray textConfigs;
    Clay_InputElementConfigArray inputConfigs;
    Clay_ScrollbarConfigArray scrollbarConfigs;
    Clay_TooltipConfigArray tooltipConfigs;
    Clay_HoverConstraintConfigArray hoverConstraintConfigs;
    Clay_TooltipArray tooltips;
    Clay_HoverConstraintConfig hoverConstraintConfigDefault;
    Clay_String fontIdDefault;
    float fontSizeDefault;
} Clay_LayoutState;

typedef struct Clay_InputState {
    Clay_Float2 mousePosition;
    bool mouseDown;
    bool mousePressed;
    float mouseWheelDelta;
    Clay_ElementId elementWithMouse;
    Clay_ElementId elementWithKeyboard;
    Clay_ElementId elementWithScroll;
    Clay_ElementId elementWithTooltip;
    Clay_ElementId elementWithHoverConstraints;
    Clay_ElementId elementBeingDragged;
} Clay_InputState;

typedef struct Clay_ArenaStateSnapshot {
    uintptr_t nextAllocation;
} Clay_ArenaStateSnapshot;

typedef struct Clay_SystemState {
    Clay_Arena arena;
    Clay_ArenaStateSnapshot arenaSnapshot;
    Clay_LayoutState layoutState;
    Clay_InputState inputState;
    Clay_ColorPaletteArray colorPalettes;
    Clay_StringArray fontIds;
    Clay_FloatArray fontSizes;
} Clay_SystemState;

typedef struct Clay_TextMeasurementCacheKey {
    uint32_t textHash;
    uint32_t fontIdHash;
    float fontSize;
    float maxWidth;
} Clay_TextMeasurementCacheKey;

typedef struct Clay_TextMeasurementCacheValue {
    float width;
    float height;
} Clay_TextMeasurementCacheValue;

typedef struct Clay_TextMeasurementCacheEntry {
    Clay_TextMeasurementCacheKey key;
    Clay_TextMeasurementCacheValue value;
} Clay_TextMeasurementCacheEntry;

typedef struct Clay_TextMeasurementCacheEntryArrayElement {
    Clay_TextMeasurementCacheEntry value;
} Clay_TextMeasurementCacheEntryArrayElement;

typedef struct Clay_TextMeasurementCacheEntryArray {
    int32_t capacity;
    int32_t length;
    Clay_TextMeasurementCacheEntryArrayElement *internalArray;
} Clay_TextMeasurementCacheEntryArray;

typedef struct Clay_TextMeasurementCache {
    Clay_TextMeasurementCacheEntryArray entries;
    uint32_t nextEvictionIndex;
} Clay_TextMeasurementCache;

typedef struct Clay__LayoutElementChildren {
    Clay_ElementId *children;
    uint16_t length;
} Clay__LayoutElementChildren;

typedef struct Clay__LayoutElementInternal {
    Clay__LayoutElementChildren children;
    Clay_TextElementData *textElementData;
    Clay_InputElementData *inputElementData;
    Clay_ScrollbarData *scrollbarData;
    Clay_TooltipData *tooltipData;
    Clay_HoverConstraintData *hoverConstraintData;
} Clay__LayoutElementInternal;

typedef struct Clay__LayoutElementInternalArrayElement {
    Clay__LayoutElementInternal value;
} Clay__LayoutElementInternalArrayElement;

typedef struct Clay__LayoutElementInternalArray {
    int32_t capacity;
    int32_t length;
    Clay__LayoutElementInternalArrayElement *internalArray;
} Clay__LayoutElementInternalArray;

typedef struct Clay__ArenaMarker {
    Clay_Arena *arena;
    uintptr_t nextAllocation;
} Clay__ArenaMarker;

typedef struct Clay__ArenaMarkerArrayElement {
    Clay__ArenaMarker value;
} Clay__ArenaMarkerArrayElement;

typedef struct Clay__ArenaMarkerArray {
    int32_t capacity;
    int32_t length;
    Clay__ArenaMarkerArrayElement *internalArray;
} Clay__ArenaMarkerArray;

typedef struct Clay__StringToIntMapEntry {
    Clay_String key;
    int32_t value;
} Clay__StringToIntMapEntry;

typedef struct Clay__StringToIntMapEntryArrayElement {
    Clay__StringToIntMapEntry value;
} Clay__StringToIntMapEntryArrayElement;

typedef struct Clay__StringToIntMapEntryArray {
    int32_t capacity;
    int32_t length;
    Clay__StringToIntMapEntryArrayElement *internalArray;
} Clay__StringToIntMapEntryArray;

typedef struct Clay__StringToIntMap {
    Clay__StringToIntMapEntryArray entries;
} Clay__StringToIntMap;

typedef struct Clay__LayoutElementChildrenSlice {
    Clay_ElementId *children;
    uint16_t length;
} Clay__LayoutElementChildrenSlice;

typedef struct Clay__LayoutElementChildrenSliceArrayElement {
    Clay__LayoutElementChildrenSlice value;
} Clay__LayoutElementChildrenSliceArrayElement;

typedef struct Clay__LayoutElementChildrenSliceArray {
    int32_t capacity;
    int32_t length;
    Clay__LayoutElementChildrenSliceArrayElement *internalArray;
} Clay__LayoutElementChildrenSliceArray;

typedef struct Clay__ElementConfigArraySlice {
    Clay_LayoutConfig *elements;
    uint16_t length;
} Clay__ElementConfigArraySlice;

typedef struct Clay__ElementConfigArraySliceArrayElement {
    Clay__ElementConfigArraySlice value;
} Clay__ElementConfigArraySliceArrayElement;

typedef struct Clay__ElementConfigArraySliceArray {
    int32_t capacity;
    int32_t length;
    Clay__ElementConfigArraySliceArrayElement *internalArray;
} Clay__ElementConfigArraySliceArray;

typedef struct {
    uint8_t rowCount;
    uint8_t selectedElementRowIndex;
} Clay__RowData;

typedef struct {
    int32_t rowCount;
    int32_t selectedElementRowIndex;
} Clay__RenderDebugLayoutData;
```
