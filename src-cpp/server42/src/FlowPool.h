#ifndef FLOW_POOL_H
#define FLOW_POOL_H

#include <map>
#include <string>
#include <vector>
#include <list>
#include <boost/shared_ptr.hpp>

namespace org::openkilda {

    struct Location {
        size_t index;
    };

    template<class A>
    class FlowPool {
        typedef typename A::value_t value_t;
        typedef std::map<std::string, Location> locator_t;
        typedef std::vector<value_t> table_t;
        typedef std::vector<std::string> flowid_table_t;

    private:
        locator_t locator;
        flowid_table_t flowid_table;

        FlowPool ( const FlowPool & ) = delete;

    public:
        table_t table;

        explicit FlowPool() {
        }

        ~FlowPool() {
            for (auto p: table) {
                A::dealocate(p);
            }
        }

        void add_flow(const std::string &flow_id, const value_t &raw_packet) {
            if (locator.find(flow_id) != locator.end()) {
                return;
            }
            locator[flow_id] = Location{table.size()};
            table.push_back(raw_packet);
            flowid_table.push_back(flow_id);
        }

        void remove_flow(const std::string &flow_id) {
            auto location_it = locator.find(flow_id);
            if (location_it == locator.end()) {
                return;
            }

            Location& location = location_it->second;

            value_t packet = table[location.index];

            table[location.index] = table.back();
            table.pop_back();

            locator[flowid_table.back()] = location;
            locator.erase(location_it);

            flowid_table[location.index] = flowid_table.back();
            flowid_table.pop_back();

            // free old mbuff
            A::dealocate(packet);
        }

        void clear() {
            for (auto p: table) {
                A::dealocate(p);
            }
            locator.clear();
            flowid_table.clear();
            table.clear();
        }

        flowid_table_t const& get_flowid_table() {
            return flowid_table;
        }
    };
}

#endif // FLOW_POOL_H
