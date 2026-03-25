#include "descriptortest.h"

namespace testlib {

static void printFields(std::ostream& out, any_ptr object, cClassDescriptor *desc, int indent);

static void tryExpand(std::ostream& out, any_ptr structPtr, const char *structName, int indent)
{
    if (structPtr == nullptr)
        return;
    cClassDescriptor *structDesc = cClassDescriptor::getDescriptorFor(structName);
    if (structDesc && structDesc->getFieldCount() > 0)
        printFields(out, structPtr, structDesc, indent);
}

static void printFields(std::ostream& out, any_ptr object, cClassDescriptor *desc, int indent)
{
    std::string ind(indent * 4, ' ');
    for (int field = 0; field < desc->getFieldCount(); field++) {
        const char *fieldName = desc->getFieldName(field);
        const char *fieldType = desc->getFieldTypeString(field);
        bool isArray = desc->getFieldIsArray(field);
        bool isCompound = desc->getFieldIsCompound(field);

        if (!isArray) {
            out << ind << fieldType << " " << fieldName << " = ";
            try { out << desc->getFieldValueAsString(object, field, 0); }
            catch (std::exception& e) { out << "ERROR: " << e.what(); }
            out << "\n";
            if (isCompound) {
                try { tryExpand(out, desc->getFieldStructValuePointer(object, field, 0), desc->getFieldStructName(field), indent + 1); }
                catch (std::exception&) {}
            }
        }
        else {
            int size = desc->getFieldArraySize(object, field);
            out << ind << fieldType << " " << fieldName << "[" << size << "]:\n";
            for (int i = 0; i < size; i++) {
                out << ind << "    " << desc->getFieldArrayIndexString(object, field, i);
                try { out << desc->getFieldValueAsString(object, field, i); }
                catch (std::exception& e) { out << "ERROR: " << e.what(); }
                out << "\n";
                if (isCompound) {
                    try { tryExpand(out, desc->getFieldStructValuePointer(object, field, i), desc->getFieldStructName(field), indent + 2); }
                    catch (std::exception&) {}
                }
            }
        }
    }
}

void printDescriptor(std::ostream& out, any_ptr object, cClassDescriptor *desc, int indent)
{
    if (!desc) {
        out << "No descriptor\n\n";
        return;
    }

    std::string ind(indent * 4, ' ');
    out << ind << "descriptor: " << desc->getName() << "\n";

    std::string valueStr = desc->getValueAsString(object);
    if (!valueStr.empty())
        out << ind << "valueAsString: " << valueStr << "\n";

    printFields(out, object, desc, indent);
    out << "\n";
}

}
