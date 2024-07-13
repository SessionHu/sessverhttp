SRC_DIR = src/java
RES_DIR = src/resources
BUILD_DIR = build

JAR_PATH = $(BUILD_DIR)/sessverhttp.jar
MANIFEST = manifest

JAVAC = javac
JAVAC_FLAGS = -d $(BUILD_DIR) -sourcepath $(SRC_DIR)

JAR = jar
JAR_FLAGS = -cvfm $(JAR_PATH) $(MANIFEST) -C $(BUILD_DIR) .

SOURCES = $(shell find $(SRC_DIR) -name "*.java")
CLASSES = $(SOURCES:$(SRC_DIR)/%.java=$(BUILD_DIR)/%.class)

ASSETS = $(shell find $(RES_DIR) -type f)
ASSETS_TARGETS = $(ASSETS:$(RES_DIR)/%=$(BUILD_DIR)/%)

# default target
all: jar

# javac
$(BUILD_DIR)/%.class: $(SRC_DIR)/%.java
	mkdir -p $(dir $@)
	$(JAVAC) $(JAVAC_FLAGS) $<

# copy assets
$(BUILD_DIR)/%: $(RES_DIR)/%
	mkdir -p $(dir $@)
	rsync -av --update $(RES_DIR)/ $(BUILD_DIR)/ && touch $@

# copy README.md
$(BUILD_DIR)/README.md: README.md
	cp README.md $(BUILD_DIR)/

# copy LICENSE
$(BUILD_DIR)/LICENSE: LICENSE
	cp LICENSE $(BUILD_DIR)/

# jar
$(JAR_PATH): $(CLASSES) $(ASSETS_TARGETS) $(BUILD_DIR)/README.md $(BUILD_DIR)/LICENSE
	rm -f $(JAR_PATH)
	$(JAR) $(JAR_FLAGS)

jar: $(JAR_PATH)

# clean
.PHONY: clean
clean:
	rm -rf $(BUILD_DIR)
