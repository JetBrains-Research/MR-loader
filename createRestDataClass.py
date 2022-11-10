import json
import sys

types = {
    "str": "String?",
    "int": "Int?",
    "bool": "Boolean?",
    "NoneType": "String?",
    "None": "String?"
}


def to_camel_case(snake_str):
    parts = snake_str.split('_')
    return parts[0] + ''.join(x.title() for x in parts[1:])

def to_class_name(snake_str):
    parts = snake_str.split('_')

    if len(parts) == 0:
        return snake_str

    return ''.join(x.title() for x in parts)


def generate(fieldName, jsonDict, space = 4):
    space_str_field = ' ' * space
    spaceStr = ' ' * (space - 4)
    print(f"{spaceStr}{to_class_name(fieldName)}(")
    for key, value in jsonDict.items():
        valueType = type(value)
        if valueType is dict:
            generate(key, value, space + 4)
            continue
        print(f"{space_str_field}@SerialName({key}) val {to_camel_case(key)}: {valueType.__name__},")

    print(f"{spaceStr})")


def generateClasses(fieldName, jsonDict):

    classes = []
    data_class = []
    space_str_field = ' ' * 2

    head = f"@Serializable data class {to_class_name(fieldName)}("
    data_class.append(head)

    for key, value in jsonDict.items():
        value_type = type(value)
        prefix = f"{space_str_field}@SerialName(\"{key}\") val "
        if value_type is dict:
            field = f"{prefix}{to_camel_case(key)}: {to_class_name(key)}?,"
            data_class.append(field)

            field_data_class, other_classes = generateClasses(key, value)
            classes.append(field_data_class)
            classes.extend(other_classes)
            continue

        elif value_type is list:
            if len(value) == 0:
                field = f"{prefix}{to_camel_case(key)}: List<String?>?,"
                data_class.append(field)
            else: 
                first_value_type = type(value[0]) 
                if first_value_type is dict:
                    field = f"{prefix}{to_camel_case(key)}: List<{to_class_name(key[:-1])}>?,"
                    data_class.append(field)
                    field_data_class, other_classes = generateClasses(key[:-1], value[0])
                    classes.append(field_data_class)
                    classes.extend(other_classes)
                else:
                    field = f"{prefix}{to_camel_case(key)}: List<{types[first_value_type.__name__]}>?,"
                    data_class.append(field)
            continue
        field = f"{prefix}{to_camel_case(key)}: {types[value_type.__name__]},"
        data_class.append(field)

    end = f")"
    data_class.append(end)

    return data_class, classes



if __name__ == "__main__" :
    decoded = {}
    path = sys.argv[1]
    with open(path, "r") as file:
        jsonText = file.read()
        decoded = json.loads(jsonText)

    data_class, classes = generateClasses(sys.argv[2], decoded)
    for line in data_class:
        print(line)

    print()

    for data_class in classes:   
        for line in data_class:
            print(line)  
        print()