export const StatCard = {
    render(title, value, icon, colorClass, delay = 0) {
        // Animation delay for staggered reveal
        const style = `animation-delay: ${delay}ms`;

        return `
            <div class="glass-card p-6 rounded-2xl animate-fade-in-up relative overflow-hidden group hover:scale-[1.02] transition-transform duration-300" style="${style}">
                <div class="absolute top-0 right-0 p-4 opacity-10 group-hover:opacity-20 transition-opacity">
                    <i class="${icon} text-6xl ${colorClass}"></i>
                </div>
                
                <div class="relative z-10">
                    <div class="flex items-center gap-4 mb-4">
                        <div class="p-3 rounded-xl bg-${colorClass.split('-')[1]}-50 dark:bg-${colorClass.split('-')[1]}-900/30 text-${colorClass.split('-')[1]}-600 dark:text-${colorClass.split('-')[1]}-400">
                            <i class="${icon} text-xl"></i>
                        </div>
                        <h3 class="text-sm font-medium text-gray-500 dark:text-gray-400 font-display">${title}</h3>
                    </div>
                    
                    <div class="flex items-baseline gap-2">
                        <span class="text-3xl font-bold text-gray-900 dark:text-white counter" data-target="${value}">0</span>
                        <span class="text-xs font-medium text-green-500 flex items-center">
                            <i class="fas fa-arrow-up mr-1"></i> 12%
                        </span>
                    </div>
                </div>
                
                <!-- Progress Line -->
                <div class="absolute bottom-0 left-0 w-full h-1 bg-gray-100 dark:bg-gray-700">
                    <div class="h-full bg-${colorClass.split('-')[1]}-500 w-[70%] rounded-r-full"></div>
                </div>
            </div>
        `;
    },

    // Function to animate counters
    animateCounters() {
        const counters = document.querySelectorAll('.counter');
        counters.forEach(counter => {
            const target = +counter.getAttribute('data-target');
            const duration = 1500; // ms
            const increment = target / (duration / 16); // 60fps

            let current = 0;
            const updateCounter = () => {
                current += increment;
                if (current < target) {
                    counter.innerText = Math.ceil(current);
                    requestAnimationFrame(updateCounter);
                } else {
                    counter.innerText = target;
                }
            };
            updateCounter();
        });
    }
};
